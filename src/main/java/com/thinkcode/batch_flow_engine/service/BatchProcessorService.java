package com.thinkcode.batch_flow_engine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkcode.batch_flow_engine.domain.entity.BatchJob;
import com.thinkcode.batch_flow_engine.domain.enums.JobStatus;
import com.thinkcode.batch_flow_engine.domain.enums.RecordStatus;
import com.thinkcode.batch_flow_engine.domain.model.BatchRecordUpdateItem;
import com.thinkcode.batch_flow_engine.domain.model.BatchTaskMessage;
import com.thinkcode.batch_flow_engine.domain.model.RecordPayloadItem;
import com.thinkcode.batch_flow_engine.domain.repository.BatchJobRepository;
import com.thinkcode.batch_flow_engine.domain.repository.BulkBatchRecordRepository;
import com.thinkcode.batch_flow_engine.exception.BatchProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BatchProcessorService {

    private static final Logger log = LoggerFactory.getLogger(BatchProcessorService.class);

    private final BatchJobRepository batchJobRepository;
    private final BulkBatchRecordRepository bulkBatchRecordRepository;
    private final ObjectMapper objectMapper;

    public BatchProcessorService(BatchJobRepository batchJobRepository,
                                 BulkBatchRecordRepository bulkBatchRecordRepository,
                                 ObjectMapper objectMapper) {
        this.batchJobRepository = batchJobRepository;
        this.bulkBatchRecordRepository = bulkBatchRecordRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes processing for a single message chunk with atomic progress tracking and failure simulation.
     */
    @Transactional
    public void processChunk(BatchTaskMessage message) {
        UUID jobId = message.getBatchJobId();
        int chunkIdx = message.getChunkIndex();
        List<RecordPayloadItem> records = message.getRecords();

        log.info("Processing chunk {}/{} for Job ID: {} ({} records, attempt #{})",
                chunkIdx + 1, message.getTotalChunks(), jobId, records.size(), message.getRetryCount() + 1);

        // 1. Mark job as PROCESSING if it was in SUBMITTED status
        batchJobRepository.startJobIfSubmitted(jobId, JobStatus.PROCESSING, OffsetDateTime.now());

        // 2. Check if chunk has a critical simulated error that should trigger exponential backoff retry
        if (message.isForceFailure()) {
            throw new BatchProcessingException(
                    "Forced chunk-level transient failure for resilience testing. Triggering exponential backoff.",
                    jobId, chunkIdx, true
            );
        }

        List<BatchRecordUpdateItem> updates = new ArrayList<>(records.size());
        int successCount = 0;
        int failedCount = 0;
        OffsetDateTime now = OffsetDateTime.now();

        for (RecordPayloadItem item : records) {
            // Check item-level simulated failure
            if (item.isSimulateFailure()) {
                // If it's simulated failure, we can either throw to fail the whole chunk or fail the item
                // When simulateFailure is set, check if we need to throw chunk-level exception
                log.warn("Record {} flagged for simulated failure (Job: {}, Chunk: {})",
                        item.getExternalId(), jobId, chunkIdx);
                
                updates.add(new BatchRecordUpdateItem(
                        item.getRecordId(),
                        RecordStatus.FAILED,
                        "Simulated business validation failure for record " + item.getExternalId(),
                        message.getRetryCount() + 1,
                        now
                ));
                failedCount++;
                continue;
            }

            // Simulate optional processing delay if specified
            if (item.getCustomDelayMs() > 0) {
                try {
                    Thread.sleep(item.getCustomDelayMs());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            try {
                // Perform record transformation / calculation
                processRecordItem(item);

                updates.add(new BatchRecordUpdateItem(
                        item.getRecordId(),
                        RecordStatus.SUCCESS,
                        null,
                        message.getRetryCount(),
                        now
                ));
                successCount++;
            } catch (Exception e) {
                log.error("Failed processing record {}: {}", item.getExternalId(), e.getMessage(), e);
                updates.add(new BatchRecordUpdateItem(
                        item.getRecordId(),
                        RecordStatus.FAILED,
                        e.getMessage(),
                        message.getRetryCount() + 1,
                        now
                ));
                failedCount++;
            }
        }

        // 3. Bulk update all record statuses in batch
        bulkBatchRecordRepository.bulkUpdateOutcomes(updates);

        // 4. Atomically increment Job progress in database
        batchJobRepository.incrementJobProgress(jobId, successCount, failedCount);

        // 5. Check if Job is fully completed
        checkAndFinalizeJob(jobId);
    }

    private void processRecordItem(RecordPayloadItem item) throws Exception {
        if (item.getData() != null && !item.getData().isBlank()) {
            JsonNode root = objectMapper.readTree(item.getData());
            // Simulated validation: ensure data is valid JSON
            if (root.has("error") && root.get("error").asBoolean()) {
                throw new IllegalArgumentException("Payload contains explicit error flag");
            }
        }
    }

    private void checkAndFinalizeJob(UUID jobId) {
        batchJobRepository.findById(jobId).ifPresent(job -> {
            long totalProcessed = job.getProcessedRecords() + job.getFailedRecords();
            if (totalProcessed >= job.getTotalRecords() && job.getStatus() == JobStatus.PROCESSING) {
                OffsetDateTime completionTime = OffsetDateTime.now();
                OffsetDateTime startTime = job.getStartedAt() != null ? job.getStartedAt() : job.getCreatedAt();
                long durationMs = Duration.between(startTime, completionTime).toMillis();

                JobStatus finalStatus;
                if (job.getFailedRecords() == 0) {
                    finalStatus = JobStatus.COMPLETED;
                } else if (job.getProcessedRecords() > 0) {
                    finalStatus = JobStatus.PARTIALLY_FAILED;
                } else {
                    finalStatus = JobStatus.FAILED;
                }

                String errorSummary = job.getFailedRecords() > 0
                        ? String.format("Job finished with %d failed records out of %d total.", job.getFailedRecords(), job.getTotalRecords())
                        : null;

                batchJobRepository.completeJob(jobId, finalStatus, completionTime, durationMs, errorSummary);

                log.info("Batch Job {} reached terminal status: {} (Duration: {} ms, Success: {}, Failed: {})",
                        jobId, finalStatus, durationMs, job.getProcessedRecords(), job.getFailedRecords());
            }
        });
    }
}
