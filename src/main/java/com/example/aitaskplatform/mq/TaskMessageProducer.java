package com.example.aitaskplatform.mq;

import com.example.aitaskplatform.config.RabbitMqConfig;
import com.example.aitaskplatform.dto.TaskMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(TaskMessage message) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.TASK_EXCHANGE, RabbitMqConfig.TASK_ROUTING_KEY, message);
    }
}
