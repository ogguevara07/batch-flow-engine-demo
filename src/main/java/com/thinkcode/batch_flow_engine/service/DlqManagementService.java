package com.thinkcode.batch_flow_engine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkcode.batch_flow_engine.amqp.BatchTaskProducer;
import com.thinkcode.batch_flow_engine.domain.entity.BatchJob;
import com.thinkcode.batch_flow_engine.domain.entity.DeadLetterRecord;
import com.thinkcode.batch_flow_engine.domain.enums.JobStatus;
import com.thinkcode.batch_flow_engine.domain.enums.RecordStatus;
import com.thinkcode.batch_flow_engine.domain.model.BatchTaskMessage;
import com.thinkcode.batch_flow_engine.domain.model.RecordPayloadItem;
import com.thinkcode.batch_flow_engine.domain.repository.BatchJobRepository;
import com.thinkcode.batch_flow_engine.domain.repository.BulkBatchRecordRepository;
import com.thinkcode.batch_flow_engine.domain.repository.DeadLetterRecordRepository;
import com.thinkcode.batch_flow_engine.dto.request.DlqRequeueRequest;
import com.thinkcode.batch_flow_engine.dto.response.DeadLetterResponse;
import com.thinkcode.batch_flow_engine.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DlqManagementService {

    private static final Logger log = LoggerFactory.getLogger(DlqManagementService.class);

    private final DeadLetterRecordRepository dlqRepository;
    private final BatchJobRepository batchJobRepository;
    private final BulkBatchRecordRepository bulkBatchRecordRepository;
    private final BatchTaskProducer batchTaskProducer;
    private final ObjectMapper objectMapper;

    public DlqManagementService(DeadLetterRecordRepository dlqRepository,
                                BatchJobRepository batchJobRepository,
                                BulkBatchRecordRepository bulkBatchRecordRepository,
                                BatchTaskProducer batchTaskProducer,
                                ObjectMapper objectMapper) {
        this.dlqRepository = dlqRepository;
        this.batchJobRepository = batchJobRepository;
        this.bulkBatchRecordRepository = bulkBatchRecordRepository;
        this.batchTaskProducer = batchTaskProducer;
        this.objectMapper = objectMapper;
    }

    /**
     * Persists a failed message arriving at the Dead Letter Queue.
     */
    @Transactional
    public DeadLetterRecord recordDeadLetter(BatchTaskMessage message,
                                            String exceptionClass,
                                            String errorMessage,
                                            String stackTrace,
                                            String originalQueue,
                                            String originalExchange,
                                            String routingKey) {

        DeadLetterRecord record = new DeadLetterRecord();
        record.setBatchJobId(message.getBatchJobId());
        record.setChunkIndex(message.getChunkIndex());
        record.setExceptionClass(exceptionClass);
        record.setErrorMessage(errorMessage);
        record.setStackTrace(stackTrace);
        record.setRetryAttempts(message.getRetryCount());
        record.setOriginalQueue(originalQueue);
        record.setOriginalExchange(originalExchange);
        record.setRoutingKey(routingKey);
        record.setResolved(false);

        try {
            record.setPayload(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            record.setPayload("{}");
        }

        DeadLetterRecord saved = dlqRepository.save(record);

        // Update individual batch records in DB to DEAD_LETTERED
        if (message.getRecords() != null && !message.getRecords().isEmpty()) {
            List<UUID> recordIds = message.getRecords().stream()
                    .map(RecordPayloadItem::getRecordId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            bulkBatchRecordRepository.bulkUpdateStatus(
                    recordIds,
                    RecordStatus.DEAD_LETTERED,
                    OffsetDateTime.now(),
                    "Moved to DLQ: " + errorMessage,
                    message.getRetryCount()
            );

            // Increment failed counter on batch job
            batchJobRepository.incrementJobProgress(message.getBatchJobId(), 0, recordIds.size());
        }

        log.warn("Recorded task in DLQ! Job ID: {}, Chunk: {}, DLQ Record ID: {}",
                message.getBatchJobId(), message.getChunkIndex(), saved.getId());

        return saved;
    }

    /**
     * Retrieves paginated DLQ records.
     */
    @Transactional(readOnly = true)
    public Page<DeadLetterResponse> getDeadLetters(Boolean resolved, Pageable pageable) {
        Page<DeadLetterRecord> page;
        if (resolved != null) {
            page = dlqRepository.findByResolvedOrderByCreatedAtDesc(resolved, pageable);
        } else {
            page = dlqRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(DeadLetterResponse::fromEntity);
    }

    /**
     * Gets a single DLQ record by ID.
     */
    @Transactional(readOnly = true)
    public DeadLetterResponse getDeadLetterById(UUID id) {
        DeadLetterRecord record = dlqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dead letter record not found with ID: " + id));
        return DeadLetterResponse.fromEntity(record);
    }

    /**
     * Requeues a single dead letter message back to the primary processing queue.
     */
    @Transactional
    public DeadLetterResponse requeueSingle(UUID deadLetterId, boolean resetRetryCount) {
        DeadLetterRecord dlq = dlqRepository.findById(deadLetterId)
                .orElseThrow(() -> new ResourceNotFoundException("Dead letter record not found: " + deadLetterId));

        if (dlq.isResolved()) {
            throw new IllegalStateException("Dead letter record is already marked as resolved.");
        }

        try {
            BatchTaskMessage taskMessage = objectMapper.readValue(dlq.getPayload(), BatchTaskMessage.class);

            if (resetRetryCount) {
                taskMessage.setRetryCount(0);
                taskMessage.setForceFailure(false);
            }

            // Reset associated batch records in database to PENDING
            if (taskMessage.getRecords() != null && !taskMessage.getRecords().isEmpty()) {
                List<UUID> recordIds = taskMessage.getRecords().stream()
                        .map(RecordPayloadItem::getRecordId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                bulkBatchRecordRepository.bulkUpdateStatus(
                        recordIds,
                        RecordStatus.PENDING,
                        null,
                        null,
                        0
                );
            }

            // Publish back to primary exchange
            batchTaskProducer.publishTask(taskMessage);

            // Mark DLQ as resolved
            dlq.setResolved(true);
            dlq.setResolvedAt(OffsetDateTime.now());
            dlq.setResolutionNotes("Manually re-enqueued to primary task queue.");
            dlq = dlqRepository.save(dlq);

            log.info("Successfully re-enqueued dead letter ID: {} back to task queue", deadLetterId);
            return DeadLetterResponse.fromEntity(dlq);
        } catch (Exception e) {
            log.error("Failed to requeue dead letter ID: {}", deadLetterId, e);
            throw new RuntimeException("Could not deserialize and requeue task: " + e.getMessage(), e);
        }
    }

    /**
     * Requeues all unresolved dead letters for a specific batch job or globally.
     */
    @Transactional
    public Map<String, Object> requeueBatch(DlqRequeueRequest request) {
        List<DeadLetterRecord> toRequeue;

        if (request.getDeadLetterIds() != null && !request.getDeadLetterIds().isEmpty()) {
            toRequeue = dlqRepository.findAllById(request.getDeadLetterIds()).stream()
                    .filter(r -> !r.isResolved())
                    .collect(Collectors.toList());
        } else if (request.getBatchJobId() != null) {
            toRequeue = dlqRepository.findByBatchJobIdAndResolved(request.getBatchJobId(), false);
        } else {
            toRequeue = Collections.emptyList();
        }

        int successCount = 0;
        int failedCount = 0;

        for (DeadLetterRecord record : toRequeue) {
            try {
                requeueSingle(record.getId(), request.isResetRetryCount());
                successCount++;
            } catch (Exception e) {
                log.error("Error requeuing DLQ ID: {}", record.getId(), e);
                failedCount++;
            }
        }

        return Map.of(
                "totalFound", toRequeue.size(),
                "requeuedSuccess", successCount,
                "requeuedFailed", failedCount
        );
    }
}
