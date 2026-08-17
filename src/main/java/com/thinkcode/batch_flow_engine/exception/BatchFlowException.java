package com.thinkcode.batch_flow_engine.exception;

public class BatchFlowException extends RuntimeException {
    public BatchFlowException(String message) {
        super(message);
    }

    public BatchFlowException(String message, Throwable cause) {
        super(message, cause);
    }
}
