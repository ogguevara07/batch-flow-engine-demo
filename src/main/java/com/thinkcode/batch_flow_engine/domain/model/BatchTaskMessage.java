package com.thinkcode.batch_flow_engine.domain.model;

import com.thinkcode.batch_flow_engine.domain.enums.TaskType;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BatchTaskMessage implements Serializable {

    private UUID messageId;
    private UUID batchJobId;
    private int chunkIndex;
    private int totalChunks;
    private TaskType taskType;
    private List<RecordPayloadItem> records = new ArrayList<>();
    private OffsetDateTime dispatchedAt;
    private int retryCount = 0;
    private boolean forceFailure = false;

    public BatchTaskMessage() {
        this.messageId = UUID.randomUUID();
        this.dispatchedAt = OffsetDateTime.now();
    }

    public BatchTaskMessage(UUID batchJobId, int chunkIndex, int totalChunks, TaskType taskType, List<RecordPayloadItem> records) {
        this.messageId = UUID.randomUUID();
        this.batchJobId = batchJobId;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.taskType = taskType;
        this.records = records != null ? records : new ArrayList<>();
        this.dispatchedAt = OffsetDateTime.now();
    }

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
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

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public List<RecordPayloadItem> getRecords() {
        return records;
    }

    public void setRecords(List<RecordPayloadItem> records) {
        this.records = records;
    }

    public OffsetDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(OffsetDateTime dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public boolean isForceFailure() {
        return forceFailure;
    }

    public void setForceFailure(boolean forceFailure) {
        this.forceFailure = forceFailure;
    }
}
