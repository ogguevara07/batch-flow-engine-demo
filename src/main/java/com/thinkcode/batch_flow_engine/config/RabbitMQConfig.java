package com.thinkcode.batch_flow_engine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
public class RabbitMQConfig {

    @Value("${batch-engine.rabbitmq.exchange:batch.direct.exchange}")
    private String exchangeName;

    @Value("${batch-engine.rabbitmq.task-queue:batch.task.processing.queue}")
    private String taskQueueName;

    @Value("${batch-engine.rabbitmq.task-routing-key:batch.task.process}")
    private String taskRoutingKey;

    @Value("${batch-engine.rabbitmq.dlx-exchange:batch.deadletter.exchange}")
    private String dlxExchangeName;

    @Value("${batch-engine.rabbitmq.dlq-queue:batch.task.deadletter.queue}")
    private String dlqQueueName;

    @Value("${batch-engine.rabbitmq.dlq-routing-key:batch.task.deadletter}")
    private String dlqRoutingKey;

    @Value("${batch-engine.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${batch-engine.retry.initial-interval-ms:1000}")
    private long initialIntervalMs;

    @Value("${batch-engine.retry.multiplier:2.0}")
    private double multiplier;

    @Value("${batch-engine.retry.max-interval-ms:10000}")
    private long maxIntervalMs;

    @Value("${spring.rabbitmq.listener.simple.concurrency:5}")
    private int concurrency;

    @Value("${spring.rabbitmq.listener.simple.max-concurrency:15}")
    private int maxConcurrency;

    @Value("${spring.rabbitmq.listener.simple.prefetch:20}")
    private int prefetchCount;

    // 1. Primary Exchange
    @Bean
    public DirectExchange primaryExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    // 2. Dead Letter Exchange (DLX)
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(dlxExchangeName, true, false);
    }

    // 3. Primary Work Queue configured with DLX
    @Bean
    public Queue primaryTaskQueue() {
        return QueueBuilder.durable(taskQueueName)
                .withArgument("x-dead-letter-exchange", dlxExchangeName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    // 4. Dead Letter Queue (DLQ)
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlqQueueName).build();
    }

    // 5. Binding: Primary Queue to Primary Exchange
    @Bean
    public Binding primaryBinding(Queue primaryTaskQueue, DirectExchange primaryExchange) {
        return BindingBuilder.bind(primaryTaskQueue).to(primaryExchange).with(taskRoutingKey);
    }

    // 6. Binding: DLQ to DLX
    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(dlqRoutingKey);
    }

    // 7. JSON Message Converter with JavaTimeModule for OffsetDateTime / Instant
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    // 8. RabbitTemplate configured with JSON Converter
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        return template;
    }

    // 9. Republish Message Recoverer: Routes exhausted retries straight into DLX
    @Bean
    public RepublishMessageRecoverer republishMessageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, dlxExchangeName, dlqRoutingKey);
    }

    // 10. Retry Operations Interceptor with Exponential Backoff
    @Bean
    public RetryOperationsInterceptor retryOperationsInterceptor(RepublishMessageRecoverer republishMessageRecoverer) {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(maxAttempts)
                .backOffOptions(initialIntervalMs, multiplier, maxIntervalMs)
                .recoverer(republishMessageRecoverer)
                .build();
    }

    // 11. Custom Listener Container Factory with Concurrency, Prefetch, and Retry Interceptor
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter,
            RetryOperationsInterceptor retryOperationsInterceptor) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(concurrency);
        factory.setMaxConcurrentConsumers(maxConcurrency);
        factory.setPrefetchCount(prefetchCount);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(retryOperationsInterceptor);
        return factory;
    }
}
