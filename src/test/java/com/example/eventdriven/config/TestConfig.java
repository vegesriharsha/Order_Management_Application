package com.example.eventdriven.config;

import com.example.eventdriven.health.RabbitMQHealthIndicator;
import com.example.eventdriven.logging.LoggingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Test configuration to provide mock beans for RabbitMQ-related services
 */
@TestConfiguration
public class TestConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(TestConfig.class);
    
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
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return mapper;
    }
    
    @Bean
    @Primary
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter(objectMapper());
    }
    
    @Bean
    @Primary
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(50);
        backOffPolicy.setMultiplier(1.0);
        backOffPolicy.setMaxInterval(100);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }
    
    @Bean
    @Primary
    public ConnectionFactory connectionFactory() {
        ConnectionFactory mockFactory = Mockito.mock(ConnectionFactory.class);
        logger.info("Created mock ConnectionFactory for tests");
        return mockFactory;
    }
    
    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = Mockito.mock(RabbitTemplate.class);
        Mockito.when(template.getConnectionFactory()).thenReturn(connectionFactory());
        logger.info("Created mock RabbitTemplate for tests");
        return template;
    }
    
    @Bean
    @Primary
    public RabbitAdmin rabbitAdmin() {
        return Mockito.mock(RabbitAdmin.class);
    }
    
    @Bean
    @Primary
    public LoggingService loggingService() {
        // Create a no-op logging service that doesn't try to use RabbitMQ
        return Mockito.mock(LoggingService.class);
    }
    
    @Bean
    @Primary
    public RabbitMQHealthIndicator rabbitMQHealthIndicator() {
        return Mockito.mock(RabbitMQHealthIndicator.class);
    }
    
    // Exchange mocks
    @Bean
    @Primary
    public DirectExchange commandExchange() {
        return new DirectExchange(commandExchange, true, false);
    }
    
    @Bean
    @Primary
    public DirectExchange eventExchange() {
        return new DirectExchange(eventExchange, true, false);
    }
    
    @Bean
    @Primary
    public FanoutExchange broadcastExchange() {
        return new FanoutExchange(broadcastExchange, true, false);
    }
    
    @Bean
    @Primary
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(deadLetterExchange, true, false);
    }
    
    @Bean
    @Primary
    public TopicExchange loggingExchange() {
        return new TopicExchange(loggingExchange, true, false);
    }
    
    // Queue mocks
    @Bean
    @Primary
    public Queue orderCommandQueue() {
        return new Queue(orderCommandQueue, true);
    }
    
    @Bean
    @Primary
    public Queue orderEventQueue() {
        return new Queue(orderEventQueue, true);
    }
    
    @Bean
    @Primary
    public Queue orderBroadcastQueue() {
        return new Queue(orderBroadcastQueue, true);
    }
    
    @Bean
    @Primary
    public Queue orderDeadLetterQueue() {
        return new Queue(orderDeadLetterQueue, true);
    }
    
    // Bindings
    @Bean
    @Primary
    public Binding orderCommandBinding() {
        return BindingBuilder.bind(orderCommandQueue())
                .to(commandExchange())
                .with(orderCommandRoutingKey);
    }

    @Bean
    @Primary
    public Binding orderEventBinding() {
        return BindingBuilder.bind(orderEventQueue())
                .to(eventExchange())
                .with(orderEventRoutingKey);
    }

    @Bean
    @Primary
    public Binding orderBroadcastBinding() {
        return BindingBuilder.bind(orderBroadcastQueue())
                .to(broadcastExchange());
    }

    @Bean
    @Primary
    public Binding orderDeadLetterBinding() {
        return BindingBuilder.bind(orderDeadLetterQueue())
                .to(deadLetterExchange())
                .with(orderCommandRoutingKey + ".dead");
    }

    @Bean
    @Primary
    public Binding orderEventDeadLetterBinding() {
        return BindingBuilder.bind(orderDeadLetterQueue())
                .to(deadLetterExchange())
                .with(orderEventRoutingKey + ".dead");
    }

    @Bean
    @Primary
    public Binding orderBroadcastDeadLetterBinding() {
        return BindingBuilder.bind(orderDeadLetterQueue())
                .to(deadLetterExchange())
                .with(orderBroadcastRoutingKey + ".dead");
    }
}