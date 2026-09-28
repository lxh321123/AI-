package com.example.aitaskplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aitaskplatform.common.ErrorCode;
import com.example.aitaskplatform.common.TaskStatus;
import com.example.aitaskplatform.common.TaskType;
import com.example.aitaskplatform.dto.CreateTaskRequest;
import com.example.aitaskplatform.dto.TaskMessage;
import com.example.aitaskplatform.dto.TaskPageRequest;
import com.example.aitaskplatform.entity.AiTask;
import com.example.aitaskplatform.entity.AiTaskResult;
import com.example.aitaskplatform.exception.BizException;
import com.example.aitaskplatform.mapper.AiTaskMapper;
import com.example.aitaskplatform.mapper.AiTaskResultMapper;
import com.example.aitaskplatform.mq.TaskMessageProducer;
import com.example.aitaskplatform.service.AiTaskService;
import com.example.aitaskplatform.service.MockAiModelService;
import com.example.aitaskplatform.service.TaskLogService;
import com.example.aitaskplatform.util.RedisKeys;
import com.example.aitaskplatform.vo.CreateTaskResponse;
import com.example.aitaskplatform.vo.TaskDetailVO;
import com.example.aitaskplatform.vo.TaskResultVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskServiceImpl implements AiTaskService {

    private static final Duration CREATE_LOCK_TTL = Duration.ofSeconds(30);
    private static final Duration TASK_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration RESULT_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration PROCESS_LOCK_TTL = Duration.ofMinutes(5);

    private final AiTaskMapper aiTaskMapper;
    private final AiTaskResultMapper aiTaskResultMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final TaskMessageProducer taskMessageProducer;
    private final MockAiModelService mockAiModelService;
    private final TaskLogService taskLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateTaskResponse createTask(CreateTaskRequest request) {
        validateTaskType(request.getTaskType());

        String cachedTaskId = stringRedisTemplate.opsForValue().get(RedisKeys.bizTask(request.getBizId()));
        if (StringUtils.hasText(cachedTaskId)) {
            return new CreateTaskResponse(cachedTaskId);
        }

        AiTask existing = selectByBizId(request.getBizId());
        if (existing != null) {
            cacheBizTask(request.getBizId(), existing.getTaskId());
            return new CreateTaskResponse(existing.getTaskId());
        }

        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(RedisKeys.createLock(request.getBizId()), "1", CREATE_LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            existing = selectByBizId(request.getBizId());
            if (existing != null) {
                return new CreateTaskResponse(existing.getTaskId());
            }
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT, "same bizId is being created, please query later");
        }

        String taskId = "AIT" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        AiTask task = new AiTask();
        task.setTaskId(taskId);
        task.setBizId(request.getBizId());
        task.setTaskName(request.getTaskName());
        task.setTaskType(request.getTaskType());
        task.setInputText(request.getInputText());
        task.setStatus(TaskStatus.PENDING.name());
        task.setPriority(request.getPriority());
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        aiTaskMapper.insert(task);
        taskLogService.record(taskId, "CREATE", "task created and waiting for async execution");

        registerAfterCommit(() -> {
            taskMessageProducer.send(new TaskMessage(taskId, request.getBizId(), request.getTaskType()));
            cacheBizTask(request.getBizId(), taskId);
            stringRedisTemplate.delete(RedisKeys.createLock(request.getBizId()));
        });
        return new CreateTaskResponse(taskId);
    }

    @Override
    public TaskDetailVO getTaskDetail(String taskId) {
        String cacheKey = RedisKeys.taskDetail(taskId);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            return readJson(cached, TaskDetailVO.class);
        }

        AiTask task = selectByTaskId(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "task not found");
        }
        TaskDetailVO detail = toDetailVO(task);
        writeJson(cacheKey, detail, TASK_CACHE_TTL);
        return detail;
    }

    @Override
    public Page<TaskDetailVO> pageTasks(TaskPageRequest request) {
        LambdaQueryWrapper<AiTask> wrapper = new LambdaQueryWrapper<AiTask>()
                .eq(StringUtils.hasText(request.getTaskStatus()), AiTask::getStatus, request.getTaskStatus())
                .eq(StringUtils.hasText(request.getTaskType()), AiTask::getTaskType, request.getTaskType())
                .orderByDesc(AiTask::getPriority)
                .orderByDesc(AiTask::getCreatedAt);

        Page<AiTask> page = aiTaskMapper.selectPage(new Page<>(request.getPageNo(), request.getPageSize()), wrapper);
        Page<TaskDetailVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toDetailVO).toList());
        return voPage;
    }

    @Override
    public TaskResultVO getTaskResult(String taskId) {
        String cacheKey = RedisKeys.taskResult(taskId);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            return readJson(cached, TaskResultVO.class);
        }

        AiTask task = selectByTaskId(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "task not found");
        }

        TaskResultVO vo = new TaskResultVO();
        vo.setTaskId(taskId);
        vo.setStatus(task.getStatus());
        vo.setFailReason(task.getFailReason());
        if (TaskStatus.SUCCESS.name().equals(task.getStatus())) {
            AiTaskResult result = selectResult(taskId);
            if (result != null) {
                vo.setResultText(result.getResultText());
                vo.setResultSummary(result.getResultSummary());
                vo.setModelName(result.getModelName());
                vo.setTokenCount(result.getTokenCount());
                vo.setCostTimeMs(result.getCostTimeMs());
                writeJson(cacheKey, vo, RESULT_CACHE_TTL);
            }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryTask(String taskId) {
        AiTask task = requireTask(taskId);
        if (!TaskStatus.FAILED.name().equals(task.getStatus())) {
            throw new BizException(ErrorCode.TASK_STATUS_ERROR, "only failed task can be retried");
        }
        if (task.getRetryCount() >= task.getMaxRetryCount()) {
            throw new BizException(ErrorCode.TOO_MANY_RETRY);
        }

        LocalDateTime now = LocalDateTime.now();
        AiTask update = new AiTask();
        update.setId(task.getId());
        update.setStatus(TaskStatus.PENDING.name());
        update.setRetryCount(task.getRetryCount() + 1);
        update.setFailReason(null);
        update.setUpdatedAt(now);
        update.setStartedAt(null);
        update.setFinishedAt(null);
        aiTaskMapper.updateById(update);
        taskLogService.record(taskId, "RETRY", "manual retry submitted");
        evictTaskCache(taskId);

        registerAfterCommit(() -> taskMessageProducer.send(new TaskMessage(taskId, task.getBizId(), task.getTaskType())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processTask(String taskId) {
        String processLockKey = RedisKeys.processLock(taskId);
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(processLockKey, "1", PROCESS_LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            log.info("task is already processing, taskId={}", taskId);
            return;
        }

        try {
            AiTask task = requireTask(taskId);
            if (TaskStatus.SUCCESS.name().equals(task.getStatus()) || selectResult(taskId) != null) {
                log.info("task has already been processed, taskId={}", taskId);
                return;
            }

            markProcessing(task);
            AiTaskResult result = mockAiModelService.execute(task);
            aiTaskResultMapper.insert(result);

            AiTask update = new AiTask();
            update.setId(task.getId());
            update.setStatus(TaskStatus.SUCCESS.name());
            update.setUpdatedAt(LocalDateTime.now());
            update.setFinishedAt(LocalDateTime.now());
            aiTaskMapper.updateById(update);
            taskLogService.record(taskId, "SUCCESS", "task processed successfully");
            evictTaskCache(taskId);
        } catch (Exception ex) {
            markFailed(taskId, ex.getMessage());
            log.warn("task process failed, taskId={}, reason={}", taskId, ex.getMessage());
        } finally {
            stringRedisTemplate.delete(processLockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markTimeoutTasksFailed(int timeoutMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<AiTask> timeoutTasks = aiTaskMapper.selectList(new LambdaQueryWrapper<AiTask>()
                .eq(AiTask::getStatus, TaskStatus.PROCESSING.name())
                .lt(AiTask::getStartedAt, threshold));
        for (AiTask task : timeoutTasks) {
            markFailed(task.getTaskId(), "task processing timeout");
        }
        return timeoutTasks.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int retryFailedTasks() {
        List<AiTask> retryableTasks = aiTaskMapper.selectList(new LambdaQueryWrapper<AiTask>()
                .eq(AiTask::getStatus, TaskStatus.FAILED.name())
                .lt(AiTask::getRetryCount, 3)
                .last("limit 20"));
        for (AiTask task : retryableTasks) {
            AiTask update = new AiTask();
            update.setId(task.getId());
            update.setStatus(TaskStatus.PENDING.name());
            update.setRetryCount(task.getRetryCount() + 1);
            update.setFailReason(null);
            update.setUpdatedAt(LocalDateTime.now());
            aiTaskMapper.updateById(update);
            taskLogService.record(task.getTaskId(), "AUTO_RETRY", "failed task auto retried by XXL-Job");
            evictTaskCache(task.getTaskId());
            registerAfterCommit(() -> taskMessageProducer.send(
                    new TaskMessage(task.getTaskId(), task.getBizId(), task.getTaskType())));
        }
        return retryableTasks.size();
    }

    private void markProcessing(AiTask task) {
        AiTask update = new AiTask();
        update.setId(task.getId());
        update.setStatus(TaskStatus.PROCESSING.name());
        update.setStartedAt(LocalDateTime.now());
        update.setUpdatedAt(LocalDateTime.now());
        aiTaskMapper.updateById(update);
        taskLogService.record(task.getTaskId(), "PROCESSING", "task consumer starts processing");
        evictTaskCache(task.getTaskId());
    }

    private void markFailed(String taskId, String reason) {
        LambdaUpdateWrapper<AiTask> wrapper = new LambdaUpdateWrapper<AiTask>()
                .eq(AiTask::getTaskId, taskId)
                .set(AiTask::getStatus, TaskStatus.FAILED.name())
                .set(AiTask::getFailReason, reason)
                .set(AiTask::getFinishedAt, LocalDateTime.now())
                .set(AiTask::getUpdatedAt, LocalDateTime.now());
        aiTaskMapper.update(wrapper);
        taskLogService.record(taskId, "FAILED", reason);
        evictTaskCache(taskId);
    }

    private AiTask requireTask(String taskId) {
        AiTask task = selectByTaskId(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "task not found");
        }
        return task;
    }

    private AiTask selectByTaskId(String taskId) {
        return aiTaskMapper.selectOne(new LambdaQueryWrapper<AiTask>().eq(AiTask::getTaskId, taskId));
    }

    private AiTask selectByBizId(String bizId) {
        return aiTaskMapper.selectOne(new LambdaQueryWrapper<AiTask>().eq(AiTask::getBizId, bizId));
    }

    private AiTaskResult selectResult(String taskId) {
        return aiTaskResultMapper.selectOne(new LambdaQueryWrapper<AiTaskResult>().eq(AiTaskResult::getTaskId, taskId));
    }

    private void validateTaskType(String taskType) {
        try {
            TaskType.valueOf(taskType);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "unsupported taskType: " + taskType);
        }
    }

    private TaskDetailVO toDetailVO(AiTask task) {
        TaskDetailVO vo = new TaskDetailVO();
        vo.setTaskId(task.getTaskId());
        vo.setBizId(task.getBizId());
        vo.setTaskName(task.getTaskName());
        vo.setTaskType(task.getTaskType());
        vo.setInputText(task.getInputText());
        vo.setStatus(task.getStatus());
        vo.setPriority(task.getPriority());
        vo.setRetryCount(task.getRetryCount());
        vo.setMaxRetryCount(task.getMaxRetryCount());
        vo.setFailReason(task.getFailReason());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        vo.setStartedAt(task.getStartedAt());
        vo.setFinishedAt(task.getFinishedAt());
        return vo;
    }

    private void cacheBizTask(String bizId, String taskId) {
        stringRedisTemplate.opsForValue().set(RedisKeys.bizTask(bizId), taskId, Duration.ofHours(6));
    }

    private void evictTaskCache(String taskId) {
        stringRedisTemplate.delete(RedisKeys.taskDetail(taskId));
        stringRedisTemplate.delete(RedisKeys.taskResult(taskId));
    }

    private <T> T readJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "cache json parse failed");
        }
    }

    private void writeJson(String key, Object value, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException ex) {
            log.warn("write redis cache failed, key={}", key, ex);
        }
    }

    private void registerAfterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
            return;
        }
        runnable.run();
    }
}
