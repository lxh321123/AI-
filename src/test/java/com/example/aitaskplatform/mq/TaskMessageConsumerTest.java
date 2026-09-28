package com.example.aitaskplatform.mq;

import com.example.aitaskplatform.dto.TaskMessage;
import com.example.aitaskplatform.service.AiTaskService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TaskMessageConsumerTest {

    @Test
    void consumeShouldDispatchTaskToService() {
        AiTaskService aiTaskService = mock(AiTaskService.class);
        TaskMessageConsumer consumer = new TaskMessageConsumer(aiTaskService);

        consumer.consume(new TaskMessage("AIT001", "biz-001", "SUMMARY"));

        verify(aiTaskService).processTask("AIT001");
    }
}
