package com.thinkcode.batch_flow_engine.exception;

import java.util.UUID;

public class BatchProcessingException extends BatchFlowException {

    private final UUID batchJobId;
    private final int chunkIndex;
    private final boolean retryable;

    public BatchProcessingException(String message, UUID batchJobId, int chunkIndex, boolean retryable) {
        super(message);
        this.batchJobId = batchJobId;
        this.chunkIndex = chunkIndex;
        this.retryable = retryable;
    }

    public BatchProcessingException(String message, Throwable cause, UUID batchJobId, int chunkIndex, boolean retryable) {
        super(message, cause);
        this.batchJobId = batchJobId;
        this.chunkIndex = chunkIndex;
        this.retryable = retryable;
    }

    public UUID getBatchJobId() {
        return batchJobId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
