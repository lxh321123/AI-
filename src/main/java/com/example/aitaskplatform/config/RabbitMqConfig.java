package com.example.aitaskplatform.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {

    public static final String TASK_EXCHANGE = "ai.task.exchange";
    public static final String TASK_QUEUE = "ai.task.queue";
    public static final String TASK_ROUTING_KEY = "ai.task.execute";
    public static final String TASK_DLX = "ai.task.dlx";
    public static final String TASK_DLQ = "ai.task.dlq";
    public static final String TASK_DLK = "ai.task.failed";

    @Bean
    public DirectExchange taskExchange() {
        return new DirectExchange(TASK_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange taskDeadLetterExchange() {
        return new DirectExchange(TASK_DLX, true, false);
    }

    @Bean
    public Queue taskQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", TASK_DLX);
        args.put("x-dead-letter-routing-key", TASK_DLK);
        return new Queue(TASK_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue taskDeadLetterQueue() {
        return new Queue(TASK_DLQ, true);
    }

    @Bean
    public Binding taskBinding(Queue taskQueue, DirectExchange taskExchange) {
        return BindingBuilder.bind(taskQueue).to(taskExchange).with(TASK_ROUTING_KEY);
    }

    @Bean
    public Binding taskDeadLetterBinding(Queue taskDeadLetterQueue, DirectExchange taskDeadLetterExchange) {
        return BindingBuilder.bind(taskDeadLetterQueue).to(taskDeadLetterExchange).with(TASK_DLK);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(4);
        return factory;
    }
}
