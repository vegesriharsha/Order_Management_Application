package com.example.eventdriven.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchanges.command}")
    private String commandExchange;

    @Value("${app.rabbitmq.exchanges.event}")
    private String eventExchange;

    @Value("${app.rabbitmq.exchanges.broadcast}")
    private String broadcastExchange;

    @Value("${app.rabbitmq.exchanges.deadletter}")
    private String deadLetterExchange;

    @Value("${app.rabbitmq.exchanges.logging}")
    private String loggingExchange;

    @Value("${app.rabbitmq.queues.orders.command}")
    private String orderCommandQueue;

    @Value("${app.rabbitmq.queues.orders.event}")
    private String orderEventQueue;

    @Value("${app.rabbitmq.queues.orders.broadcast}")
    private String orderBroadcastQueue;

    @Value("${app.rabbitmq.queues.orders.deadletter}")
    private String orderDeadLetterQueue;

    @Value("${app.rabbitmq.routing-keys.orders.command}")
    private String orderCommandRoutingKey;

    @Value("${app.rabbitmq.routing-keys.orders.event}")
    private String orderEventRoutingKey;

    @Value("${app.rabbitmq.routing-keys.orders.broadcast}")
    private String orderBroadcastRoutingKey;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter, RetryTemplate retryTemplate) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        rabbitTemplate.setRetryTemplate(retryTemplate);
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    // Exchanges
    @Bean
    public DirectExchange commandExchange() {
        return new DirectExchange(commandExchange, true, false);
    }

    @Bean
    public DirectExchange eventExchange() {
        return new DirectExchange(eventExchange, true, false);
    }

    @Bean
    public FanoutExchange broadcastExchange() {
        return new FanoutExchange(broadcastExchange, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(deadLetterExchange, true, false);
    }

    @Bean
    public TopicExchange loggingExchange() {
        return new TopicExchange(loggingExchange, true, false);
    }

    // Dead Letter Queues
    @Bean
    public Queue orderDeadLetterQueue() {
        return QueueBuilder.durable(orderDeadLetterQueue)
                .build();
    }

    // Command Queue with DLQ
    @Bean
    public Queue orderCommandQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", deadLetterExchange);
        args.put("x-dead-letter-routing-key", orderCommandRoutingKey + ".dead");
        return QueueBuilder.durable(orderCommandQueue)
                .withArguments(args)
                .build();
    }

    // Event Queue with DLQ
    @Bean
    public Queue orderEventQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", deadLetterExchange);
        args.put("x-dead-letter-routing-key", orderEventRoutingKey + ".dead");
        return QueueBuilder.durable(orderEventQueue)
                .withArguments(args)
                .build();
    }

    // Broadcast Queue with DLQ
    @Bean
    public Queue orderBroadcastQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", deadLetterExchange);
        args.put("x-dead-letter-routing-key", orderBroadcastRoutingKey + ".dead");
        return QueueBuilder.durable(orderBroadcastQueue)
                .withArguments(args)
                .build();
    }

    // Bindings
    @Bean
    public Binding orderCommandBinding() {
        return BindingBuilder.bind(orderCommandQueue())
                .to(commandExchange())
                .with(orderCommandRoutingKey);
    }

    @Bean
    public Binding orderEventBinding() {
        return BindingBuilder.bind(orderEventQueue())
                .to(eventExchange())
                .with(orderEventRoutingKey);
    }

    @Bean
    public Binding orderBroadcastBinding() {
        return BindingBuilder.bind(orderBroadcastQueue())
                .to(broadcastExchange());
    }

    @Bean
    public Binding orderDeadLetterBinding() {
        return BindingBuilder.bind(orderDeadLetterQueue())
                .to(deadLetterExchange())
                .with(orderCommandRoutingKey + ".dead");
    }

    @Bean
    public Binding orderEventDeadLetterBinding() {
        return BindingBuilder.bind(orderDeadLetterQueue())
                .to(deadLetterExchange())
                .with(orderEventRoutingKey + ".dead");
    }

    @Bean
    public Binding orderBroadcastDeadLetterBinding() {
        return BindingBuilder.bind(orderDeadLetterQueue())
                .to(deadLetterExchange())
                .with(orderBroadcastRoutingKey + ".dead");
    }
}
