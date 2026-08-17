package com.thinkcode.batch_flow_engine.service;

import com.thinkcode.batch_flow_engine.domain.entity.BatchJob;
import com.thinkcode.batch_flow_engine.domain.enums.JobStatus;
import com.thinkcode.batch_flow_engine.domain.repository.BatchJobRepository;
import com.thinkcode.batch_flow_engine.domain.repository.DeadLetterRecordRepository;
import com.thinkcode.batch_flow_engine.dto.response.EngineMetricsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class BatchMetricsService {

    private final BatchJobRepository batchJobRepository;
    private final DeadLetterRecordRepository dlqRepository;

    @Value("${spring.rabbitmq.listener.simple.concurrency:5}")
    private int concurrency;

    @Value("${spring.rabbitmq.listener.simple.max-concurrency:15}")
    private int maxConcurrency;

    @Value("${spring.rabbitmq.listener.simple.prefetch:20}")
    private int prefetch;

    @Value("${batch-engine.retry.max-attempts:3}")
    private int maxRetryAttempts;

    public BatchMetricsService(BatchJobRepository batchJobRepository, DeadLetterRecordRepository dlqRepository) {
        this.batchJobRepository = batchJobRepository;
        this.dlqRepository = dlqRepository;
    }

    @Transactional(readOnly = true)
    public EngineMetricsResponse getMetrics() {
        List<BatchJob> allJobs = batchJobRepository.findAll();

        long submitted = 0;
        long completed = 0;
        long failed = 0;
        long processing = 0;
        long totalProcessedRecords = 0;
        long totalFailedRecords = 0;

        for (BatchJob job : allJobs) {
            if (job.getStatus() == JobStatus.SUBMITTED) submitted++;
            else if (job.getStatus() == JobStatus.COMPLETED) completed++;
            else if (job.getStatus() == JobStatus.FAILED || job.getStatus() == JobStatus.PARTIALLY_FAILED) failed++;
            else if (job.getStatus() == JobStatus.PROCESSING) processing++;

            totalProcessedRecords += job.getProcessedRecords();
            totalFailedRecords += job.getFailedRecords();
        }

        long activeDlq = dlqRepository.countByResolved(false);
        long resolvedDlq = dlqRepository.countByResolved(true);

        long grandTotalRecords = totalProcessedRecords + totalFailedRecords;
        double successRate = grandTotalRecords > 0
                ? (double) totalProcessedRecords / grandTotalRecords * 100.0
                : 100.0;

        EngineMetricsResponse response = new EngineMetricsResponse();
        response.setTotalJobsSubmitted(allJobs.size());
        response.setTotalJobsCompleted(completed);
        response.setTotalJobsFailed(failed);
        response.setTotalJobsProcessing(processing);
        response.setTotalRecordsProcessed(totalProcessedRecords);
        response.setTotalRecordsFailed(totalFailedRecords);
        response.setActiveDeadLetters(activeDlq);
        response.setResolvedDeadLetters(resolvedDlq);
        response.setOverallSuccessRatePercentage(Math.round(successRate * 100.0) / 100.0);
        response.setTimestamp(OffsetDateTime.now());

        response.setQueueMetrics(Map.of(
                "workerConcurrency", concurrency,
                "workerMaxConcurrency", maxConcurrency,
                "workerPrefetch", prefetch,
                "maxRetryAttempts", maxRetryAttempts
        ));

        return response;
    }
}
