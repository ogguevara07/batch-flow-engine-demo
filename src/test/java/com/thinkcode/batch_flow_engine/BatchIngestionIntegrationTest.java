package com.thinkcode.batch_flow_engine;

import com.thinkcode.batch_flow_engine.amqp.BatchTaskProducer;
import com.thinkcode.batch_flow_engine.domain.entity.BatchJob;
import com.thinkcode.batch_flow_engine.domain.entity.BatchRecord;
import com.thinkcode.batch_flow_engine.domain.enums.JobStatus;
import com.thinkcode.batch_flow_engine.domain.enums.RecordStatus;
import com.thinkcode.batch_flow_engine.domain.repository.BatchJobRepository;
import com.thinkcode.batch_flow_engine.domain.repository.BatchRecordRepository;
import com.thinkcode.batch_flow_engine.domain.repository.BulkBatchRecordRepository;
import com.thinkcode.batch_flow_engine.dto.request.BatchSubmissionRequest;
import com.thinkcode.batch_flow_engine.dto.request.GenerateTestBatchRequest;
import com.thinkcode.batch_flow_engine.dto.response.BatchJobResponse;
import com.thinkcode.batch_flow_engine.service.BatchIngestionService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class BatchIngestionIntegrationTest {

    @Autowired
    private BatchIngestionService batchIngestionService;

    @Autowired
    private BatchJobRepository batchJobRepository;

    @Autowired
    private BatchRecordRepository batchRecordRepository;

    @Autowired
    private BulkBatchRecordRepository bulkBatchRecordRepository;

    @MockBean
    private BatchTaskProducer batchTaskProducer;

    @BeforeEach
    void setup() {
        batchRecordRepository.deleteAll();
        batchJobRepository.deleteAll();
    }

    @Test
    @DisplayName("Should bulk insert and chunk a massive batch of 2,000 records")
    void testMassiveBatchIngestion() {
        int totalRecords = 2000;
        int chunkSize = 250;

        GenerateTestBatchRequest request = new GenerateTestBatchRequest(
                "Massive-Ingestion-Test",
                totalRecords,
                chunkSize,
                0 // 0% failure
        );

        long startTime = System.currentTimeMillis();
        BatchJobResponse response = batchIngestionService.generateSyntheticBatch(request);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getTotalRecords()).isEqualTo(totalRecords);
        assertThat(response.getChunkSize()).isEqualTo(chunkSize);
        assertThat(response.getStatus()).isEqualTo(JobStatus.SUBMITTED);

        // Verify database persistence
        BatchJob savedJob = batchJobRepository.findById(response.getId()).orElse(null);
        assertThat(savedJob).isNotNull();
        assertThat(savedJob.getTotalRecords()).isEqualTo(totalRecords);

        // Verify records created
        long count = batchRecordRepository.countByBatchJobId(response.getId());
        assertThat(count).isEqualTo(totalRecords);

        // Verify all records have PENDING status initially
        long pendingCount = batchRecordRepository.countByBatchJobIdAndStatus(response.getId(), RecordStatus.PENDING);
        assertThat(pendingCount).isEqualTo(totalRecords);

        // Verify message producer was called for each chunk (2000 / 250 = 8 chunks)
        verify(batchTaskProducer, atLeastOnce()).publishTask(any());

        System.out.println("Ingested and persisted " + totalRecords + " records in " + duration + " ms");
    }

    @Test
    @DisplayName("Should handle custom batch submission with metadata")
    void testCustomBatchSubmission() {
        BatchSubmissionRequest request = new BatchSubmissionRequest();
        request.setJobName("Custom-Financial-Batch");
        request.setChunkSize(10);

        List<BatchSubmissionRequest.ItemInput> items = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            BatchSubmissionRequest.ItemInput item = new BatchSubmissionRequest.ItemInput(
                    "EXT-" + (100 + i),
                    "{\"amount\": " + (50.0 + i) + ", \"currency\": \"EUR\"}"
            );
            items.add(item);
        }
        request.setItems(items);

        BatchJobResponse response = batchIngestionService.submitBatch(request);

        assertThat(response).isNotNull();
        assertThat(response.getTotalRecords()).isEqualTo(25);
        assertThat(response.getJobName()).isEqualTo("Custom-Financial-Batch");

        long savedCount = batchRecordRepository.countByBatchJobId(response.getId());
        assertThat(savedCount).isEqualTo(25);
    }
}
