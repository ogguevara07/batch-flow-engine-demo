package com.thinkcode.batch_flow_engine.dto.response;

import com.thinkcode.batch_flow_engine.domain.entity.BatchRecord;
import com.thinkcode.batch_flow_engine.domain.enums.RecordStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BatchRecordResponse {

    private UUID id;
    private UUID batchJobId;
    private int chunkIndex;
    private String externalId;
    private String payload;
    private RecordStatus status;
    private int retryCount;
    private String errorMessage;
    private OffsetDateTime processedAt;
    private OffsetDateTime createdAt;

    public BatchRecordResponse() {
    }

    public static BatchRecordResponse fromEntity(BatchRecord record) {
        if (record == null) return null;
        BatchRecordResponse resp = new BatchRecordResponse();
        resp.setId(record.getId());
        resp.setBatchJobId(record.getBatchJobId());
        resp.setChunkIndex(record.getChunkIndex());
        resp.setExternalId(record.getExternalId());
        resp.setPayload(record.getPayload());
        resp.setStatus(record.getStatus());
        resp.setRetryCount(record.getRetryCount());
        resp.setErrorMessage(record.getErrorMessage());
        resp.setProcessedAt(record.getProcessedAt());
        resp.setCreatedAt(record.getCreatedAt());
        return resp;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBatchJobId() {
        return batchJobId;
    }

    public void setBatchJobId(UUID batchJobId) {
        this.batchJobId = batchJobId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
