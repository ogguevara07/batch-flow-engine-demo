package com.thinkcode.batch_flow_engine;

import com.thinkcode.batch_flow_engine.amqp.BatchTaskProducer;
import com.thinkcode.batch_flow_engine.domain.entity.BatchJob;
import com.thinkcode.batch_flow_engine.domain.entity.BatchRecord;
import com.thinkcode.batch_flow_engine.domain.entity.DeadLetterRecord;
import com.thinkcode.batch_flow_engine.domain.enums.JobStatus;
import com.thinkcode.batch_flow_engine.domain.enums.RecordStatus;
import com.thinkcode.batch_flow_engine.domain.enums.TaskType;
import com.thinkcode.batch_flow_engine.domain.model.BatchTaskMessage;
import com.thinkcode.batch_flow_engine.domain.model.RecordPayloadItem;
import com.thinkcode.batch_flow_engine.domain.repository.BatchJobRepository;
import com.thinkcode.batch_flow_engine.domain.repository.BatchRecordRepository;
import com.thinkcode.batch_flow_engine.domain.repository.BulkBatchRecordRepository;
import com.thinkcode.batch_flow_engine.domain.repository.DeadLetterRecordRepository;
import com.thinkcode.batch_flow_engine.dto.response.DeadLetterResponse;
import com.thinkcode.batch_flow_engine.exception.BatchProcessingException;
import com.thinkcode.batch_flow_engine.service.BatchProcessorService;
import com.thinkcode.batch_flow_engine.service.DlqManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class BatchProcessorResilienceTest {

    @Autowired
    private BatchProcessorService batchProcessorService;

    @Autowired
    private DlqManagementService dlqManagementService;

    @Autowired
    private BatchJobRepository batchJobRepository;

    @Autowired
    private BatchRecordRepository batchRecordRepository;

    @Autowired
    private BulkBatchRecordRepository bulkBatchRecordRepository;

    @Autowired
    private DeadLetterRecordRepository deadLetterRecordRepository;

    @MockBean
    private BatchTaskProducer batchTaskProducer;

    private BatchJob testJob;
    private List<BatchRecord> testRecords;

    @BeforeEach
    void setup() {
        deadLetterRecordRepository.deleteAll();
        batchRecordRepository.deleteAll();
        batchJobRepository.deleteAll();

        testJob = new BatchJob("Resilience-Test-Job", 10, 10, null);
        testJob = batchJobRepository.save(testJob);

        testRecords = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            BatchRecord record = new BatchRecord(
                    testJob.getId(),
                    0,
                    "EXT-" + (i + 1),
                    "{\"amount\": " + (100.0 * (i + 1)) + "}"
            );
            testRecords.add(record);
        }
        bulkBatchRecordRepository.bulkInsert(testRecords, 50);
    }

    @Test
    @DisplayName("Should successfully process chunk and finalize batch job to COMPLETED")
    void testSuccessfulChunkProcessing() {
        List<RecordPayloadItem> payloadItems = new ArrayList<>();
        for (BatchRecord r : testRecords) {
            payloadItems.add(new RecordPayloadItem(r.getId(), r.getExternalId(), r.getPayload()));
        }

        BatchTaskMessage message = new BatchTaskMessage(
                testJob.getId(),
                0,
                1,
                TaskType.DATA_TRANSFORMATION,
                payloadItems
        );

        batchProcessorService.processChunk(message);

        // Verify batch job transitioned to COMPLETED
        BatchJob updatedJob = batchJobRepository.findById(testJob.getId()).orElseThrow();
        assertThat(updatedJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(updatedJob.getProcessedRecords()).isEqualTo(10);
        assertThat(updatedJob.getFailedRecords()).isEqualTo(0);
        assertThat(updatedJob.getDurationMs()).isNotNull();

        // Verify records in DB updated to SUCCESS
        long successCount = batchRecordRepository.countByBatchJobIdAndStatus(testJob.getId(), RecordStatus.SUCCESS);
        assertThat(successCount).isEqualTo(10);
    }

    @Test
    @DisplayName("Should trigger exception on forced chunk failure for exponential backoff")
    void testForcedFailureTriggeringRetry() {
        List<RecordPayloadItem> payloadItems = new ArrayList<>();
        for (BatchRecord r : testRecords) {
            payloadItems.add(new RecordPayloadItem(r.getId(), r.getExternalId(), r.getPayload()));
        }

        BatchTaskMessage message = new BatchTaskMessage(
                testJob.getId(),
                0,
                1,
                TaskType.DATA_TRANSFORMATION,
                payloadItems
        );
        message.setForceFailure(true);

        assertThatThrownBy(() -> batchProcessorService.processChunk(message))
                .isInstanceOf(BatchProcessingException.class)
                .hasMessageContaining("exponential backoff");
    }

    @Test
    @DisplayName("Should persist dead letter record and support DLQ re-enqueuing")
    void testDeadLetterPersistenceAndRequeue() {
        List<RecordPayloadItem> payloadItems = new ArrayList<>();
        for (BatchRecord r : testRecords) {
            payloadItems.add(new RecordPayloadItem(r.getId(), r.getExternalId(), r.getPayload()));
        }

        BatchTaskMessage message = new BatchTaskMessage(
                testJob.getId(),
                0,
                1,
                TaskType.DATA_TRANSFORMATION,
                payloadItems
        );
        message.setRetryCount(3); // exhausted retry attempts

        // 1. Record dead letter
        DeadLetterRecord dlq = dlqManagementService.recordDeadLetter(
                message,
                "SimulatedTimeoutException",
                "Downstream database connection timeout after 3 retries",
                "java.util.concurrent.TimeoutException\n\tat com.thinkcode...",
                "batch.task.processing.queue",
                "batch.direct.exchange",
                "batch.task.process"
        );

        assertThat(dlq).isNotNull();
        assertThat(dlq.getId()).isNotNull();
        assertThat(dlq.isResolved()).isFalse();
        assertThat(dlq.getRetryAttempts()).isEqualTo(3);

        // Verify records in DB marked as DEAD_LETTERED
        long deadLetteredCount = batchRecordRepository.countByBatchJobIdAndStatus(testJob.getId(), RecordStatus.DEAD_LETTERED);
        assertThat(deadLetteredCount).isEqualTo(10);

        // 2. Requeue dead letter
        DeadLetterResponse requeueResponse = dlqManagementService.requeueSingle(dlq.getId(), true);
        assertThat(requeueResponse.isResolved()).isTrue();
        assertThat(requeueResponse.getResolvedAt()).isNotNull();

        // Verify producer published message back to queue
        verify(batchTaskProducer).publishTask(any());

        // Verify records in DB reset to PENDING
        long pendingCount = batchRecordRepository.countByBatchJobIdAndStatus(testJob.getId(), RecordStatus.PENDING);
        assertThat(pendingCount).isEqualTo(10);
    }
}
