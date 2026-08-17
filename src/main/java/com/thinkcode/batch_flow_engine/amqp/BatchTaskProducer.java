package com.thinkcode.batch_flow_engine.amqp;

import com.thinkcode.batch_flow_engine.domain.model.BatchTaskMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BatchTaskProducer {

    private static final Logger log = LoggerFactory.getLogger(BatchTaskProducer.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${batch-engine.rabbitmq.exchange:batch.direct.exchange}")
    private String exchangeName;

    @Value("${batch-engine.rabbitmq.task-routing-key:batch.task.process}")
    private String taskRoutingKey;

    public BatchTaskProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishTask(BatchTaskMessage message) {
        log.debug("Publishing batch task message ID: {} for Job: {}, Chunk: {}/{}",
                message.getMessageId(), message.getBatchJobId(), message.getChunkIndex() + 1, message.getTotalChunks());

        rabbitTemplate.convertAndSend(exchangeName, taskRoutingKey, message, m -> {
            m.getMessageProperties().setMessageId(message.getMessageId().toString());
            m.getMessageProperties().setCorrelationId(message.getBatchJobId().toString());
            m.getMessageProperties().setHeader("chunkIndex", message.getChunkIndex());
            m.getMessageProperties().setHeader("totalChunks", message.getTotalChunks());
            m.getMessageProperties().setHeader("retryCount", message.getRetryCount());
            return m;
        });
    }
}
