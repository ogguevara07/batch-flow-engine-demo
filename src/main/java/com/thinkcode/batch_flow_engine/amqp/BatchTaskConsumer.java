package com.thinkcode.batch_flow_engine.amqp;

import com.rabbitmq.client.Channel;
import com.thinkcode.batch_flow_engine.domain.model.BatchTaskMessage;
import com.thinkcode.batch_flow_engine.service.BatchProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class BatchTaskConsumer {

    private static final Logger log = LoggerFactory.getLogger(BatchTaskConsumer.class);

    private final BatchProcessorService batchProcessorService;

    public BatchTaskConsumer(BatchProcessorService batchProcessorService) {
        this.batchProcessorService = batchProcessorService;
    }

    @RabbitListener(
            queues = "${batch-engine.rabbitmq.task-queue:batch.task.processing.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleBatchTask(
            @Payload BatchTaskMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(value = "x-delivery-count", required = false) Long deliveryCount) throws IOException {

        log.info("Worker received task chunk {} for Job ID: {} (Delivery Tag: {})",
                message.getChunkIndex() + 1, message.getBatchJobId(), deliveryTag);

        try {
            batchProcessorService.processChunk(message);
            channel.basicAck(deliveryTag, false);
            log.info("Task chunk {} for Job ID: {} successfully acknowledged",
                    message.getChunkIndex() + 1, message.getBatchJobId());
        } catch (Exception ex) {
            log.error("Error processing task chunk {} for Job ID: {}. Error: {}",
                    message.getChunkIndex() + 1, message.getBatchJobId(), ex.getMessage());

            // By throwing the exception, Spring AMQP RetryOperationsInterceptor triggers exponential backoff.
            // When max attempts are exceeded, RepublishMessageRecoverer automatically routes the message
            // to the Dead Letter Exchange (DLX) with complete stack trace and error headers.
            throw ex;
        }
    }
}
