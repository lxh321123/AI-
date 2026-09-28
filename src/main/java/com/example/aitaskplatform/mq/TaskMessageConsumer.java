package com.example.aitaskplatform.mq;

import com.example.aitaskplatform.config.RabbitMqConfig;
import com.example.aitaskplatform.dto.TaskMessage;
import com.example.aitaskplatform.service.AiTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskMessageConsumer {

    private final AiTaskService aiTaskService;

    @RabbitListener(queues = RabbitMqConfig.TASK_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void consume(TaskMessage message) {
        MDC.put("taskId", message.getTaskId());
        MDC.put("bizId", message.getBizId());
        try {
            log.info("receive ai task message, taskId={}, taskType={}", message.getTaskId(), message.getTaskType());
            aiTaskService.processTask(message.getTaskId());
        } finally {
            MDC.clear();
        }
    }
}
