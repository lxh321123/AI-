package com.example.aitaskplatform.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.aitaskplatform.common.TaskStatus;
import com.example.aitaskplatform.common.TaskType;
import com.example.aitaskplatform.dto.CreateTaskRequest;
import com.example.aitaskplatform.entity.AiTask;
import com.example.aitaskplatform.entity.AiTaskResult;
import com.example.aitaskplatform.mapper.AiTaskMapper;
import com.example.aitaskplatform.mapper.AiTaskResultMapper;
import com.example.aitaskplatform.mq.TaskMessageProducer;
import com.example.aitaskplatform.service.impl.AiTaskServiceImpl;
import com.example.aitaskplatform.util.RedisKeys;
import com.example.aitaskplatform.vo.CreateTaskResponse;
import com.example.aitaskplatform.vo.TaskDetailVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiTaskServiceImplTest {

    @Mock
    private AiTaskMapper aiTaskMapper;
    @Mock
    private AiTaskResultMapper aiTaskResultMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private TaskMessageProducer taskMessageProducer;
    @Mock
    private MockAiModelService mockAiModelService;
    @Mock
    private TaskLogService taskLogService;

    private AiTaskServiceImpl aiTaskService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        aiTaskService = new AiTaskServiceImpl(
                aiTaskMapper,
                aiTaskResultMapper,
                stringRedisTemplate,
                objectMapper,
                taskMessageProducer,
                mockAiModelService,
                taskLogService
        );
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void createTaskShouldReturnExistingTaskWhenBizIdCached() {
        CreateTaskRequest request = createRequest("biz-001");
        when(valueOperations.get(RedisKeys.bizTask("biz-001"))).thenReturn("AITEXISTING001");

        CreateTaskResponse response = aiTaskService.createTask(request);

        assertThat(response.getTaskId()).isEqualTo("AITEXISTING001");
        verify(aiTaskMapper, never()).insert(any(AiTask.class));
        verify(taskMessageProducer, never()).send(any());
    }

    @Test
    void createTaskShouldInsertTaskAndSendMessage() {
        CreateTaskRequest request = createRequest("biz-002");
        when(valueOperations.get(RedisKeys.bizTask("biz-002"))).thenReturn(null);
        when(valueOperations.setIfAbsent(eq(RedisKeys.createLock("biz-002")), eq("1"), any(Duration.class))).thenReturn(true);

        CreateTaskResponse response = aiTaskService.createTask(request);

        assertThat(response.getTaskId()).startsWith("AIT");
        ArgumentCaptor<AiTask> taskCaptor = ArgumentCaptor.forClass(AiTask.class);
        verify(aiTaskMapper).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getStatus()).isEqualTo(TaskStatus.PENDING.name());
        verify(taskMessageProducer).send(any());
    }

    @Test
    void getTaskDetailShouldHitCacheFirst() throws Exception {
        TaskDetailVO cached = new TaskDetailVO();
        cached.setTaskId("AITCACHE001");
        cached.setStatus(TaskStatus.SUCCESS.name());
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        when(valueOperations.get(RedisKeys.taskDetail("AITCACHE001"))).thenReturn(objectMapper.writeValueAsString(cached));

        TaskDetailVO result = aiTaskService.getTaskDetail("AITCACHE001");

        assertThat(result.getTaskId()).isEqualTo("AITCACHE001");
        verify(aiTaskMapper, never()).selectOne(any(Wrapper.class));
    }

    @Test
    void processTaskShouldMoveTaskToSuccess() {
        AiTask task = buildTask("AITPROCESS001", TaskStatus.PENDING);
        AiTaskResult result = new AiTaskResult();
        result.setTaskId("AITPROCESS001");
        result.setResultText("ok");
        result.setResultSummary("ok");
        result.setModelName("mock");
        result.setTokenCount(12);
        result.setCostTimeMs(100L);
        result.setCreatedAt(LocalDateTime.now());
        result.setUpdatedAt(LocalDateTime.now());

        when(valueOperations.setIfAbsent(eq(RedisKeys.processLock("AITPROCESS001")), eq("1"), any(Duration.class))).thenReturn(true);
        when(aiTaskMapper.selectOne(any(Wrapper.class))).thenReturn(task);
        when(aiTaskResultMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(mockAiModelService.execute(task)).thenReturn(result);

        aiTaskService.processTask("AITPROCESS001");

        verify(aiTaskResultMapper).insert(result);
        verify(aiTaskMapper, times(2)).updateById(any(AiTask.class));
        verify(stringRedisTemplate).delete(RedisKeys.processLock("AITPROCESS001"));
    }

    @Test
    void processTaskShouldSkipWhenResultAlreadyExists() {
        AiTask task = buildTask("AITDUP001", TaskStatus.PROCESSING);
        AiTaskResult existingResult = new AiTaskResult();
        existingResult.setTaskId("AITDUP001");

        when(valueOperations.setIfAbsent(eq(RedisKeys.processLock("AITDUP001")), eq("1"), any(Duration.class))).thenReturn(true);
        when(aiTaskMapper.selectOne(any(Wrapper.class))).thenReturn(task);
        when(aiTaskResultMapper.selectOne(any(Wrapper.class))).thenReturn(existingResult);

        aiTaskService.processTask("AITDUP001");

        verify(mockAiModelService, never()).execute(any());
        verify(aiTaskResultMapper, never()).insert(any(AiTaskResult.class));
    }

    private CreateTaskRequest createRequest(String bizId) {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTaskName("summary task");
        request.setTaskType(TaskType.SUMMARY.name());
        request.setInputText("please summarize this paragraph");
        request.setBizId(bizId);
        request.setPriority(5);
        return request;
    }

    private AiTask buildTask(String taskId, TaskStatus status) {
        AiTask task = new AiTask();
        task.setId(1L);
        task.setTaskId(taskId);
        task.setBizId("biz-" + taskId);
        task.setTaskName("test task");
        task.setTaskType(TaskType.SUMMARY.name());
        task.setInputText("input");
        task.setStatus(status.name());
        task.setPriority(5);
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }
}
