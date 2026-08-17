package com.thinkcode.batch_flow_engine.dto.response;

import com.thinkcode.batch_flow_engine.domain.entity.BatchJob;
import com.thinkcode.batch_flow_engine.domain.enums.JobStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BatchJobResponse {

    private UUID id;
    private String jobName;
    private JobStatus status;
    private long totalRecords;
    private long processedRecords;
    private long failedRecords;
    private double progressPercentage;
    private int chunkSize;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private Long durationMs;
    private String errorSummary;
    private String metadata;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public BatchJobResponse() {
    }

    public static BatchJobResponse fromEntity(BatchJob job) {
        if (job == null) return null;
        BatchJobResponse resp = new BatchJobResponse();
        resp.setId(job.getId());
        resp.setJobName(job.getJobName());
        resp.setStatus(job.getStatus());
        resp.setTotalRecords(job.getTotalRecords());
        resp.setProcessedRecords(job.getProcessedRecords());
        resp.setFailedRecords(job.getFailedRecords());
        resp.setChunkSize(job.getChunkSize());
        resp.setStartedAt(job.getStartedAt());
        resp.setCompletedAt(job.getCompletedAt());
        resp.setDurationMs(job.getDurationMs());
        resp.setErrorSummary(job.getErrorSummary());
        resp.setMetadata(job.getMetadata());
        resp.setCreatedAt(job.getCreatedAt());
        resp.setUpdatedAt(job.getUpdatedAt());

        long processedTotal = job.getProcessedRecords() + job.getFailedRecords();
        if (job.getTotalRecords() > 0) {
            double percent = (double) processedTotal / job.getTotalRecords() * 100.0;
            resp.setProgressPercentage(Math.round(percent * 100.0) / 100.0);
        } else {
            resp.setProgressPercentage(0.0);
        }

        return resp;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public long getProcessedRecords() {
        return processedRecords;
    }

    public void setProcessedRecords(long processedRecords) {
        this.processedRecords = processedRecords;
    }

    public long getFailedRecords() {
        return failedRecords;
    }

    public void setFailedRecords(long failedRecords) {
        this.failedRecords = failedRecords;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
