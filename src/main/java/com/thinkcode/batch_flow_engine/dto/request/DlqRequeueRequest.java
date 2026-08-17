package com.thinkcode.batch_flow_engine.dto.request;

import java.util.List;
import java.util.UUID;

public class DlqRequeueRequest {

    private UUID deadLetterId;
    private UUID batchJobId;
    private List<UUID> deadLetterIds;
    private boolean resetRetryCount = true;

    public DlqRequeueRequest() {
    }

    public UUID getDeadLetterId() {
        return deadLetterId;
    }

    public void setDeadLetterId(UUID deadLetterId) {
        this.deadLetterId = deadLetterId;
    }

    public UUID getBatchJobId() {
        return batchJobId;
    }

    public void setBatchJobId(UUID batchJobId) {
        this.batchJobId = batchJobId;
    }

    public List<UUID> getDeadLetterIds() {
        return deadLetterIds;
    }

    public void setDeadLetterIds(List<UUID> deadLetterIds) {
        this.deadLetterIds = deadLetterIds;
    }

    public boolean isResetRetryCount() {
        return resetRetryCount;
    }

    public void setResetRetryCount(boolean resetRetryCount) {
        this.resetRetryCount = resetRetryCount;
    }
}
