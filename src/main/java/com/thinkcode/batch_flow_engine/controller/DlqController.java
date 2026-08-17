package com.thinkcode.batch_flow_engine.controller;

import com.thinkcode.batch_flow_engine.dto.request.DlqRequeueRequest;
import com.thinkcode.batch_flow_engine.dto.response.ApiResponse;
import com.thinkcode.batch_flow_engine.dto.response.DeadLetterResponse;
import com.thinkcode.batch_flow_engine.service.DlqManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dlq")
@Tag(name = "Dead Letter Queue (DLQ)", description = "Endpoints for inspecting failed messages, analyzing failure causes, and replaying dead letters")
public class DlqController {

    private final DlqManagementService dlqManagementService;

    public DlqController(DlqManagementService dlqManagementService) {
        this.dlqManagementService = dlqManagementService;
    }

    @GetMapping
    @Operation(summary = "List dead letter messages", description = "Retrieves paginated messages residing in or recorded by the DLQ")
    public ResponseEntity<ApiResponse<Page<DeadLetterResponse>>> listDeadLetters(
            @RequestParam(required = false) Boolean resolved,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<DeadLetterResponse> response = dlqManagementService.getDeadLetters(resolved, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dead letter diagnostics", description = "Fetches complete payload, stack trace, exception class and retry count for a dead letter item")
    public ResponseEntity<ApiResponse<DeadLetterResponse>> getDeadLetterById(@PathVariable UUID id) {
        DeadLetterResponse response = dlqManagementService.getDeadLetterById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/requeue")
    @Operation(summary = "Requeue single dead letter", description = "Re-publishes a failed task back into the primary processing queue and marks it as resolved")
    public ResponseEntity<ApiResponse<DeadLetterResponse>> requeueSingle(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean resetRetryCount) {

        DeadLetterResponse response = dlqManagementService.requeueSingle(id, resetRetryCount);
        return ResponseEntity.ok(ApiResponse.success("Message successfully re-enqueued to primary queue", response));
    }

    @PostMapping("/requeue-batch")
    @Operation(summary = "Bulk requeue dead letters", description = "Replays multiple dead letters filtered by batchJobId or list of deadLetterIds")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requeueBatch(@RequestBody DlqRequeueRequest request) {
        Map<String, Object> result = dlqManagementService.requeueBatch(request);
        return ResponseEntity.ok(ApiResponse.success("Bulk requeue operation executed", result));
    }
}
