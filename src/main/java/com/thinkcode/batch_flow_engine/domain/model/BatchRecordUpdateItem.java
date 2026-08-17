package com.thinkcode.batch_flow_engine.domain.model;

import com.thinkcode.batch_flow_engine.domain.enums.RecordStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BatchRecordUpdateItem {
    private UUID recordId;
    private RecordStatus status;
    private String errorMessage;
    private int retryCount;
    private OffsetDateTime processedAt;

    public BatchRecordUpdateItem() {
    }

    public BatchRecordUpdateItem(UUID recordId, RecordStatus status, String errorMessage, int retryCount, OffsetDateTime processedAt) {
        this.recordId = recordId;
        this.status = status;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.processedAt = processedAt;
    }

    public UUID getRecordId() {
        return recordId;
    }

    public void setRecordId(UUID recordId) {
        this.recordId = recordId;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
