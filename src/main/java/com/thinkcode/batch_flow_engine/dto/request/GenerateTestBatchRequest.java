package com.thinkcode.batch_flow_engine.dto.request;

import com.thinkcode.batch_flow_engine.domain.enums.TaskType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class GenerateTestBatchRequest {

    @NotBlank(message = "Job name prefix is required")
    private String jobNamePrefix = "Synthetic-Batch";

    @Min(value = 1, message = "Total records must be at least 1")
    @Max(value = 500000, message = "Total records cannot exceed 500,000 in a single test batch")
    private int totalRecords = 5000;

    @Min(value = 1, message = "Chunk size must be at least 1")
    @Max(value = 5000, message = "Chunk size cannot exceed 5,000")
    private int chunkSize = 200;

    @Min(value = 0, message = "Failure percentage must be between 0 and 100")
    @Max(value = 100, message = "Failure percentage must be between 0 and 100")
    private int failurePercentage = 0;

    private int simulatedDelayPerRecordMs = 0;

    private TaskType taskType = TaskType.DATA_TRANSFORMATION;

    public GenerateTestBatchRequest() {
    }

    public GenerateTestBatchRequest(String jobNamePrefix, int totalRecords, int chunkSize, int failurePercentage) {
        this.jobNamePrefix = jobNamePrefix;
        this.totalRecords = totalRecords;
        this.chunkSize = chunkSize;
        this.failurePercentage = failurePercentage;
    }

    public String getJobNamePrefix() {
        return jobNamePrefix;
    }

    public void setJobNamePrefix(String jobNamePrefix) {
        this.jobNamePrefix = jobNamePrefix;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getFailurePercentage() {
        return failurePercentage;
    }

    public void setFailurePercentage(int failurePercentage) {
        this.failurePercentage = failurePercentage;
    }

    public int getSimulatedDelayPerRecordMs() {
        return simulatedDelayPerRecordMs;
    }

    public void setSimulatedDelayPerRecordMs(int simulatedDelayPerRecordMs) {
        this.simulatedDelayPerRecordMs = simulatedDelayPerRecordMs;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }
}
