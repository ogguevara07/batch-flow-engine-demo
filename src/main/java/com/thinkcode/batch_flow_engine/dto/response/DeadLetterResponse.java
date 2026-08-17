package com.thinkcode.batch_flow_engine.dto.response;

import com.thinkcode.batch_flow_engine.domain.entity.DeadLetterRecord;

import java.time.OffsetDateTime;
import java.util.UUID;

public class DeadLetterResponse {

    private UUID id;
    private UUID batchJobId;
    private UUID recordId;
    private Integer chunkIndex;
    private String payload;
    private String exceptionClass;
    private String errorMessage;
    private String stackTrace;
    private int retryAttempts;
    private String originalQueue;
    private String originalExchange;
    private String routingKey;
    private boolean resolved;
    private OffsetDateTime resolvedAt;
    private String resolutionNotes;
    private OffsetDateTime createdAt;

    public DeadLetterResponse() {
    }

    public static DeadLetterResponse fromEntity(DeadLetterRecord dlq) {
        if (dlq == null) return null;
        DeadLetterResponse resp = new DeadLetterResponse();
        resp.setId(dlq.getId());
        resp.setBatchJobId(dlq.getBatchJobId());
        resp.setRecordId(dlq.getRecordId());
        resp.setChunkIndex(dlq.getChunkIndex());
        resp.setPayload(dlq.getPayload());
        resp.setExceptionClass(dlq.getExceptionClass());
        resp.setErrorMessage(dlq.getErrorMessage());
        resp.setStackTrace(dlq.getStackTrace());
        resp.setRetryAttempts(dlq.getRetryAttempts());
        resp.setOriginalQueue(dlq.getOriginalQueue());
        resp.setOriginalExchange(dlq.getOriginalExchange());
        resp.setRoutingKey(dlq.getRoutingKey());
        resp.setResolved(dlq.isResolved());
        resp.setResolvedAt(dlq.getResolvedAt());
        resp.setResolutionNotes(dlq.getResolutionNotes());
        resp.setCreatedAt(dlq.getCreatedAt());
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

    public UUID getRecordId() {
        return recordId;
    }

    public void setRecordId(UUID recordId) {
        this.recordId = recordId;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public String getOriginalQueue() {
        return originalQueue;
    }

    public void setOriginalQueue(String originalQueue) {
        this.originalQueue = originalQueue;
    }

    public String getOriginalExchange() {
        return originalExchange;
    }

    public void setOriginalExchange(String originalExchange) {
        this.originalExchange = originalExchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
