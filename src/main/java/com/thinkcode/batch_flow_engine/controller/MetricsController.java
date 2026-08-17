package com.thinkcode.batch_flow_engine.controller;

import com.thinkcode.batch_flow_engine.dto.response.ApiResponse;
import com.thinkcode.batch_flow_engine.dto.response.EngineMetricsResponse;
import com.thinkcode.batch_flow_engine.service.BatchMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metrics")
@Tag(name = "Engine Metrics", description = "Endpoints for monitoring batch engine performance, throughput, and error rates")
public class MetricsController {

    private final BatchMetricsService batchMetricsService;

    public MetricsController(BatchMetricsService batchMetricsService) {
        this.batchMetricsService = batchMetricsService;
    }

    @GetMapping("/engine")
    @Operation(summary = "Get Batch Engine Metrics", description = "Calculates total throughput, success/failure ratio, DLQ size, and active worker parameters")
    public ResponseEntity<ApiResponse<EngineMetricsResponse>> getEngineMetrics() {
        EngineMetricsResponse response = batchMetricsService.getMetrics();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
