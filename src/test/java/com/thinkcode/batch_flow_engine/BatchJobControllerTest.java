package com.thinkcode.batch_flow_engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkcode.batch_flow_engine.amqp.BatchTaskProducer;
import com.thinkcode.batch_flow_engine.domain.entity.BatchJob;
import com.thinkcode.batch_flow_engine.domain.enums.JobStatus;
import com.thinkcode.batch_flow_engine.domain.repository.BatchJobRepository;
import com.thinkcode.batch_flow_engine.dto.request.BatchSubmissionRequest;
import com.thinkcode.batch_flow_engine.dto.request.GenerateTestBatchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BatchJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BatchJobRepository batchJobRepository;

    @MockBean
    private BatchTaskProducer batchTaskProducer;

    @BeforeEach
    void setup() {
        batchJobRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/batches - Submit custom batch")
    void testSubmitBatchEndpoint() throws Exception {
        BatchSubmissionRequest request = new BatchSubmissionRequest();
        request.setJobName("API-Integration-Job");
        request.setChunkSize(50);
        request.setItems(List.of(
                new BatchSubmissionRequest.ItemInput("EXT-1", "{\"value\": 100}"),
                new BatchSubmissionRequest.ItemInput("EXT-2", "{\"value\": 200}")
        ));

        mockMvc.perform(post("/api/v1/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobName").value("API-Integration-Job"))
                .andExpect(jsonPath("$.data.totalRecords").value(2))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("POST /api/v1/batches/generate - Generate synthetic workload")
    void testGenerateSyntheticBatchEndpoint() throws Exception {
        GenerateTestBatchRequest request = new GenerateTestBatchRequest(
                "Synthetic-Test",
                100,
                20,
                5
        );

        mockMvc.perform(post("/api/v1/batches/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRecords").value(100))
                .andExpect(jsonPath("$.data.chunkSize").value(20));
    }

    @Test
    @DisplayName("GET /api/v1/batches/{id} - Get batch job status")
    void testGetBatchStatus() throws Exception {
        BatchJob job = new BatchJob("Status-Query-Job", 100, 500, null);
        job.setStatus(JobStatus.PROCESSING);
        job.setProcessedRecords(250);
        job = batchJobRepository.save(job);

        mockMvc.perform(get("/api/v1/batches/{id}", job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(job.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andExpect(jsonPath("$.data.progressPercentage").value(50.0));
    }

    @Test
    @DisplayName("GET /api/v1/metrics/engine - Retrieve engine operational metrics")
    void testGetEngineMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/metrics/engine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.queueMetrics.workerConcurrency").value(2))
                .andExpect(jsonPath("$.data.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/dlq - List dead letter messages")
    void testListDlq() throws Exception {
        mockMvc.perform(get("/api/v1/dlq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
