package com.thinkcode.batch_flow_engine.dto.response;

import java.time.OffsetDateTime;
import java.util.Map;

public class EngineMetricsResponse {

    private long totalJobsSubmitted;
    private long totalJobsCompleted;
    private long totalJobsFailed;
    private long totalJobsProcessing;
    private long totalRecordsProcessed;
    private long totalRecordsFailed;
    private long activeDeadLetters;
    private long resolvedDeadLetters;
    private double overallSuccessRatePercentage;
    private Map<String, Object> queueMetrics;
    private OffsetDateTime timestamp;

    public EngineMetricsResponse() {
        this.timestamp = OffsetDateTime.now();
    }

    public long getTotalJobsSubmitted() {
        return totalJobsSubmitted;
    }

    public void setTotalJobsSubmitted(long totalJobsSubmitted) {
        this.totalJobsSubmitted = totalJobsSubmitted;
    }

    public long getTotalJobsCompleted() {
        return totalJobsCompleted;
    }

    public void setTotalJobsCompleted(long totalJobsCompleted) {
        this.totalJobsCompleted = totalJobsCompleted;
    }

    public long getTotalJobsFailed() {
        return totalJobsFailed;
    }

    public void setTotalJobsFailed(long totalJobsFailed) {
        this.totalJobsFailed = totalJobsFailed;
    }

    public long getTotalJobsProcessing() {
        return totalJobsProcessing;
    }

    public void setTotalJobsProcessing(long totalJobsProcessing) {
        this.totalJobsProcessing = totalJobsProcessing;
    }

    public long getTotalRecordsProcessed() {
        return totalRecordsProcessed;
    }

    public void setTotalRecordsProcessed(long totalRecordsProcessed) {
        this.totalRecordsProcessed = totalRecordsProcessed;
    }

    public long getTotalRecordsFailed() {
        return totalRecordsFailed;
    }

    public void setTotalRecordsFailed(long totalRecordsFailed) {
        this.totalRecordsFailed = totalRecordsFailed;
    }

    public long getActiveDeadLetters() {
        return activeDeadLetters;
    }

    public void setActiveDeadLetters(long activeDeadLetters) {
        this.activeDeadLetters = activeDeadLetters;
    }

    public long getResolvedDeadLetters() {
        return resolvedDeadLetters;
    }

    public void setResolvedDeadLetters(long resolvedDeadLetters) {
        this.resolvedDeadLetters = resolvedDeadLetters;
    }

    public double getOverallSuccessRatePercentage() {
        return overallSuccessRatePercentage;
    }

    public void setOverallSuccessRatePercentage(double overallSuccessRatePercentage) {
        this.overallSuccessRatePercentage = overallSuccessRatePercentage;
    }

    public Map<String, Object> getQueueMetrics() {
        return queueMetrics;
    }

    public void setQueueMetrics(Map<String, Object> queueMetrics) {
        this.queueMetrics = queueMetrics;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
