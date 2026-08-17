package com.thinkcode.batch_flow_engine.domain.model;

import java.io.Serializable;
import java.util.UUID;

public class RecordPayloadItem implements Serializable {
    private UUID recordId;
    private String externalId;
    private String data;
    private boolean simulateFailure;
    private int customDelayMs;

    public RecordPayloadItem() {
    }

    public RecordPayloadItem(UUID recordId, String externalId, String data) {
        this.recordId = recordId;
        this.externalId = externalId;
        this.data = data;
    }

    public RecordPayloadItem(UUID recordId, String externalId, String data, boolean simulateFailure, int customDelayMs) {
        this.recordId = recordId;
        this.externalId = externalId;
        this.data = data;
        this.simulateFailure = simulateFailure;
        this.customDelayMs = customDelayMs;
    }

    public UUID getRecordId() {
        return recordId;
    }

    public void setRecordId(UUID recordId) {
        this.recordId = recordId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    public int getCustomDelayMs() {
        return customDelayMs;
    }

    public void setCustomDelayMs(int customDelayMs) {
        this.customDelayMs = customDelayMs;
    }
}
