package com.thinkcode.batch_flow_engine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkcode.batch_flow_engine.amqp.BatchTaskProducer;
import com.thinkcode.batch_flow_engine.domain.entity.BatchJob;
import com.thinkcode.batch_flow_engine.domain.entity.BatchRecord;
import com.thinkcode.batch_flow_engine.domain.enums.JobStatus;
import com.thinkcode.batch_flow_engine.domain.enums.TaskType;
import com.thinkcode.batch_flow_engine.domain.model.BatchTaskMessage;
import com.thinkcode.batch_flow_engine.domain.model.RecordPayloadItem;
import com.thinkcode.batch_flow_engine.domain.repository.BatchJobRepository;
import com.thinkcode.batch_flow_engine.domain.repository.BulkBatchRecordRepository;
import com.thinkcode.batch_flow_engine.dto.request.BatchSubmissionRequest;
import com.thinkcode.batch_flow_engine.dto.request.GenerateTestBatchRequest;
import com.thinkcode.batch_flow_engine.dto.response.BatchJobResponse;
import com.thinkcode.batch_flow_engine.exception.BatchFlowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class BatchIngestionService {

    private static final Logger log = LoggerFactory.getLogger(BatchIngestionService.class);

    private final BatchJobRepository batchJobRepository;
    private final BulkBatchRecordRepository bulkBatchRecordRepository;
    private final BatchTaskProducer batchTaskProducer;
    private final ObjectMapper objectMapper;

    @Value("${batch-engine.chunk-size:200}")
    private int defaultChunkSize;

    @Value("${batch-engine.jdbc-batch-size:1000}")
    private int jdbcBatchSize;

    public BatchIngestionService(BatchJobRepository batchJobRepository,
                                 BulkBatchRecordRepository bulkBatchRecordRepository,
                                 BatchTaskProducer batchTaskProducer,
                                 ObjectMapper objectMapper) {
        this.batchJobRepository = batchJobRepository;
        this.bulkBatchRecordRepository = bulkBatchRecordRepository;
        this.batchTaskProducer = batchTaskProducer;
        this.objectMapper = objectMapper;
    }

    /**
     * Ingests a user-submitted batch job with specified payload items.
     */
    @Transactional
    public BatchJobResponse submitBatch(BatchSubmissionRequest request) {
        int chunkSize = request.getChunkSize() != null && request.getChunkSize() > 0
                ? request.getChunkSize()
                : defaultChunkSize;

        List<BatchSubmissionRequest.ItemInput> items = request.getItems() != null
                ? request.getItems()
                : Collections.emptyList();

        String metadataJson = null;
        if (request.getMetadata() != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(request.getMetadata());
            } catch (JsonProcessingException e) {
                log.warn("Could not serialize metadata map", e);
            }
        }

        BatchJob job = new BatchJob(request.getJobName(), chunkSize, items.size(), metadataJson);
        job = batchJobRepository.save(job);

        if (!items.isEmpty()) {
            ingestAndPublishRecords(job, items, request.getTaskType(), chunkSize);
        } else {
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(OffsetDateTime.now());
            job.setDurationMs(0L);
            job = batchJobRepository.save(job);
        }

        log.info("Batch job created successfully. ID: {}, Name: '{}', Total Records: {}",
                job.getId(), job.getJobName(), job.getTotalRecords());

        return BatchJobResponse.fromEntity(job);
    }

    /**
     * Generates a high-volume synthetic batch for stress and resiliency testing.
     */
    @Transactional
    public BatchJobResponse generateSyntheticBatch(GenerateTestBatchRequest request) {
        int total = request.getTotalRecords();
        int chunkSize = request.getChunkSize() > 0 ? request.getChunkSize() : defaultChunkSize;
        int failurePct = request.getFailurePercentage();

        String jobName = request.getJobNamePrefix() + "-" + System.currentTimeMillis();
        Map<String, Object> meta = Map.of(
                "type", "SYNTHETIC_BENCHMARK",
                "totalRecords", total,
                "failurePercentage", failurePct,
                "simulatedDelayMs", request.getSimulatedDelayPerRecordMs()
        );

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException e) {
            metadataJson = "{}";
        }

        BatchJob job = new BatchJob(jobName, chunkSize, total, metadataJson);
        job = batchJobRepository.save(job);

        List<BatchSubmissionRequest.ItemInput> items = new ArrayList<>(total);
        Random random = new Random();

        for (int i = 0; i < total; i++) {
            BatchSubmissionRequest.ItemInput item = new BatchSubmissionRequest.ItemInput();
            item.setExternalId("EXT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

            Map<String, Object> payloadMap = Map.of(
                    "index", i + 1,
                    "accountNumber", "ACC-" + (100000 + i),
                    "amount", Math.round((10.0 + random.nextDouble() * 5000.0) * 100.0) / 100.0,
                    "currency", "USD",
                    "timestamp", OffsetDateTime.now().toString()
            );

            try {
                item.setData(objectMapper.writeValueAsString(payloadMap));
            } catch (JsonProcessingException e) {
                item.setData("{\"index\":" + (i + 1) + "}");
            }

            boolean shouldFail = failurePct > 0 && (random.nextInt(100) < failurePct);
            item.setSimulateFailure(shouldFail);
            item.setCustomDelayMs(request.getSimulatedDelayPerRecordMs());
            items.add(item);
        }

        ingestAndPublishRecords(job, items, request.getTaskType(), chunkSize);

        log.info("Synthetic batch generated. Job ID: {}, Total Records: {}, Failure Rate: {}%",
                job.getId(), total, failurePct);

        return BatchJobResponse.fromEntity(job);
    }

    private void ingestAndPublishRecords(BatchJob job, List<BatchSubmissionRequest.ItemInput> items, TaskType taskType, int chunkSize) {
        int totalItems = items.size();
        int totalChunks = (int) Math.ceil((double) totalItems / chunkSize);

        List<BatchRecord> recordsToInsert = new ArrayList<>(totalItems);
        Map<Integer, List<RecordPayloadItem>> chunkPayloads = new HashMap<>();

        for (int i = 0; i < totalItems; i++) {
            int chunkIndex = i / chunkSize;
            BatchSubmissionRequest.ItemInput input = items.get(i);

            UUID recordId = UUID.randomUUID();
            BatchRecord record = new BatchRecord(job.getId(), chunkIndex, input.getExternalId(), input.getData());
            record.setId(recordId);
            recordsToInsert.add(record);

            RecordPayloadItem payloadItem = new RecordPayloadItem(
                    recordId,
                    input.getExternalId(),
                    input.getData(),
                    input.isSimulateFailure(),
                    input.getCustomDelayMs()
            );

            chunkPayloads.computeIfAbsent(chunkIndex, k -> new ArrayList<>()).add(payloadItem);
        }

        // 1. Bulk insert all records into database via optimized JDBC batching
        long startInsert = System.currentTimeMillis();
        bulkBatchRecordRepository.bulkInsert(recordsToInsert, jdbcBatchSize);
        long insertDuration = System.currentTimeMillis() - startInsert;
        log.info("Persisted {} batch records in {} ms (Job ID: {})", totalItems, insertDuration, job.getId());

        // 2. Publish chunked messages to RabbitMQ
        for (Map.Entry<Integer, List<RecordPayloadItem>> entry : chunkPayloads.entrySet()) {
            int chunkIndex = entry.getKey();
            List<RecordPayloadItem> chunkRecords = entry.getValue();

            BatchTaskMessage message = new BatchTaskMessage(
                    job.getId(),
                    chunkIndex,
                    totalChunks,
                    taskType != null ? taskType : TaskType.DATA_TRANSFORMATION,
                    chunkRecords
            );

            batchTaskProducer.publishTask(message);
        }

        log.info("Dispatched {} message chunks to RabbitMQ for Job ID: {}", totalChunks, job.getId());
    }
}
