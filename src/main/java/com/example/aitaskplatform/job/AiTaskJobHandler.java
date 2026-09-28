package com.example.aitaskplatform.job;

import com.example.aitaskplatform.service.AiTaskService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiTaskJobHandler {

    private final AiTaskService aiTaskService;

    @XxlJob("markTimeoutAiTaskJob")
    @Scheduled(fixedDelayString = "${task.scheduler.timeout-scan-delay-ms:300000}")
    public void markTimeoutAiTaskJob() {
        int count = aiTaskService.markTimeoutTasksFailed(10);
        log.info("timeout task scan finished, count={}", count);
    }

    @XxlJob("retryFailedAiTaskJob")
    @Scheduled(fixedDelayString = "${task.scheduler.failed-retry-delay-ms:600000}")
    public void retryFailedAiTaskJob() {
        int count = aiTaskService.retryFailedTasks();
        log.info("failed task retry scan finished, count={}", count);
    }
}
