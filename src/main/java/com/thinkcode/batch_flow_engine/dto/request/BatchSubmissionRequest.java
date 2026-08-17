package com.thinkcode.batch_flow_engine.dto.request;

import com.thinkcode.batch_flow_engine.domain.enums.TaskType;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public class BatchSubmissionRequest {

    @NotBlank(message = "Job name is required")
    private String jobName;

    private TaskType taskType = TaskType.DATA_TRANSFORMATION;

    private Integer chunkSize = 100;

    private Map<String, Object> metadata;

    private List<ItemInput> items;

    public static class ItemInput {
        private String externalId;
        private String data;
        private boolean simulateFailure;
        private int customDelayMs;

        public ItemInput() {
        }

        public ItemInput(String externalId, String data) {
            this.externalId = externalId;
            this.data = data;
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

    public BatchSubmissionRequest() {
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<ItemInput> getItems() {
        return items;
    }

    public void setItems(List<ItemInput> items) {
        this.items = items;
    }
}
