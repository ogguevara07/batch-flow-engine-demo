package com.thinkcode.batch_flow_engine.controller;

import com.thinkcode.batch_flow_engine.domain.entity.BatchJob;
import com.thinkcode.batch_flow_engine.domain.entity.BatchRecord;
import com.thinkcode.batch_flow_engine.domain.enums.JobStatus;
import com.thinkcode.batch_flow_engine.domain.enums.RecordStatus;
import com.thinkcode.batch_flow_engine.domain.repository.BatchJobRepository;
import com.thinkcode.batch_flow_engine.domain.repository.BatchRecordRepository;
import com.thinkcode.batch_flow_engine.dto.request.BatchSubmissionRequest;
import com.thinkcode.batch_flow_engine.dto.request.GenerateTestBatchRequest;
import com.thinkcode.batch_flow_engine.dto.response.ApiResponse;
import com.thinkcode.batch_flow_engine.dto.response.BatchJobResponse;
import com.thinkcode.batch_flow_engine.dto.response.BatchRecordResponse;
import com.thinkcode.batch_flow_engine.exception.ResourceNotFoundException;
import com.thinkcode.batch_flow_engine.service.BatchIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/batches")
@Tag(name = "Batch Jobs", description = "Endpoints for batch job submission, execution monitoring, and record queries")
public class BatchJobController {

    private final BatchIngestionService batchIngestionService;
    private final BatchJobRepository batchJobRepository;
    private final BatchRecordRepository batchRecordRepository;

    public BatchJobController(BatchIngestionService batchIngestionService,
                              BatchJobRepository batchJobRepository,
                              BatchRecordRepository batchRecordRepository) {
        this.batchIngestionService = batchIngestionService;
        this.batchJobRepository = batchJobRepository;
        this.batchRecordRepository = batchRecordRepository;
    }

    @PostMapping
    @Operation(summary = "Submit a new batch job", description = "Ingests a custom collection of records, stores them in PostgreSQL in batch, chunks them and dispatches to RabbitMQ")
    public ResponseEntity<ApiResponse<BatchJobResponse>> submitBatch(@Valid @RequestBody BatchSubmissionRequest request) {
        BatchJobResponse response = batchIngestionService.submitBatch(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Batch job submitted for asynchronous processing", response));
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate synthetic mass workload", description = "Creates a large-scale test batch (e.g. 5,000 - 100,000 items) with optional failure rate simulation for stress testing")
    public ResponseEntity<ApiResponse<BatchJobResponse>> generateSyntheticBatch(@Valid @RequestBody GenerateTestBatchRequest request) {
        BatchJobResponse response = batchIngestionService.generateSyntheticBatch(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Synthetic batch workload generated and dispatched to queue", response));
    }

    @GetMapping
    @Operation(summary = "List all batch jobs", description = "Returns a paginated list of batch jobs, optionally filtered by status")
    public ResponseEntity<ApiResponse<Page<BatchJobResponse>>> listBatches(
            @RequestParam(required = false) JobStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<BatchJob> page = (status != null)
                ? batchJobRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : batchJobRepository.findAllByOrderByCreatedAtDesc(pageable);

        Page<BatchJobResponse> responsePage = page.map(BatchJobResponse::fromEntity);
        return ResponseEntity.ok(ApiResponse.success(responsePage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get batch job status", description = "Retrieves live execution progress, duration, record counts, and status for a given batch job ID")
    public ResponseEntity<ApiResponse<BatchJobResponse>> getBatchStatus(@PathVariable UUID id) {
        BatchJob job = batchJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch job not found with ID: " + id));
        return ResponseEntity.ok(ApiResponse.success(BatchJobResponse.fromEntity(job)));
    }

    @GetMapping("/{id}/records")
    @Operation(summary = "Get records for a batch job", description = "Returns paginated individual records of a batch job with error messages and processing states")
    public ResponseEntity<ApiResponse<Page<BatchRecordResponse>>> getBatchRecords(
            @PathVariable UUID id,
            @RequestParam(required = false) RecordStatus status,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {

        // Verify job exists
        if (!batchJobRepository.existsById(id)) {
            throw new ResourceNotFoundException("Batch job not found with ID: " + id);
        }

        Page<BatchRecord> records = (status != null)
                ? batchRecordRepository.findByBatchJobIdAndStatusOrderByCreatedAtAsc(id, status, pageable)
                : batchRecordRepository.findByBatchJobIdOrderByCreatedAtAsc(id, pageable);

        return ResponseEntity.ok(ApiResponse.success(records.map(BatchRecordResponse::fromEntity)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a batch job", description = "Cancels a currently queued or processing batch job")
    public ResponseEntity<ApiResponse<BatchJobResponse>> cancelBatch(@PathVariable UUID id) {
        BatchJob job = batchJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch job not found with ID: " + id));

        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.CANCELLED) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Job cannot be cancelled as it is already in status: " + job.getStatus()));
        }

        job.setStatus(JobStatus.CANCELLED);
        job.setCompletedAt(OffsetDateTime.now());
        job.setErrorSummary("Job was manually cancelled by user.");
        job = batchJobRepository.save(job);

        return ResponseEntity.ok(ApiResponse.success("Batch job successfully cancelled", BatchJobResponse.fromEntity(job)));
    }
}
