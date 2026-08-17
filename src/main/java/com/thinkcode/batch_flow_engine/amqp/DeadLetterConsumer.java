package com.thinkcode.batch_flow_engine.amqp;

import com.rabbitmq.client.Channel;
import com.thinkcode.batch_flow_engine.domain.model.BatchTaskMessage;
import com.thinkcode.batch_flow_engine.service.DlqManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    private final DlqManagementService dlqManagementService;

    public DeadLetterConsumer(DlqManagementService dlqManagementService) {
        this.dlqManagementService = dlqManagementService;
    }

    @RabbitListener(
            queues = "${batch-engine.rabbitmq.dlq-queue:batch.task.deadletter.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleDeadLetterMessage(
            @Payload BatchTaskMessage message,
            @Headers Map<String, Object> headers,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.warn("Dead Letter Queue received failed message: ID={}, Job ID={}, Chunk={}",
                message.getMessageId(), message.getBatchJobId(), message.getChunkIndex());

        String exceptionClass = getHeaderString(headers, "x-exception-class-name");
        String errorMessage = getHeaderString(headers, "x-exception-message");
        String stackTrace = getHeaderString(headers, "x-exception-stacktrace");
        String originalQueue = getHeaderString(headers, "x-original-queue");
        String originalExchange = getHeaderString(headers, "x-original-exchange");
        String routingKey = getHeaderString(headers, "x-original-routing-key");

        if (errorMessage == null || errorMessage.isBlank()) {
            errorMessage = "Task exceeded maximum retry threshold without success";
        }

        try {
            dlqManagementService.recordDeadLetter(
                    message,
                    exceptionClass != null ? exceptionClass : "BatchProcessingException",
                    errorMessage,
                    stackTrace,
                    originalQueue,
                    originalExchange,
                    routingKey
            );

            // Acknowledge from DLQ as the incident is safely persisted in the relational database
            channel.basicAck(deliveryTag, false);
            log.info("Dead letter message acknowledged and persisted. DeliveryTag: {}", deliveryTag);
        } catch (Exception ex) {
            log.error("Fatal error storing dead letter message to database", ex);
            channel.basicNack(deliveryTag, false, true); // Re-queue in DLQ if DB is temporarily unreachable
        }
    }

    private String getHeaderString(Map<String, Object> headers, String key) {
        Object val = headers.get(key);
        return val != null ? val.toString() : null;
    }
}
