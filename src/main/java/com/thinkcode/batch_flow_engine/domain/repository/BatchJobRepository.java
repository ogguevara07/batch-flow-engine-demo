package com.thinkcode.batch_flow_engine.domain.repository;

import com.thinkcode.batch_flow_engine.domain.entity.BatchJob;
import com.thinkcode.batch_flow_engine.domain.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BatchJobRepository extends JpaRepository<BatchJob, UUID> {

    Page<BatchJob> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<BatchJob> findByStatusOrderByCreatedAtDesc(JobStatus status, Pageable pageable);

    List<BatchJob> findByStatusIn(List<JobStatus> statuses);

    @Modifying
    @Query("UPDATE BatchJob j SET j.processedRecords = j.processedRecords + :processedCount, " +
            "j.failedRecords = j.failedRecords + :failedCount " +
            "WHERE j.id = :jobId")
    int incrementJobProgress(@Param("jobId") UUID jobId,
                             @Param("processedCount") long processedCount,
                             @Param("failedCount") long failedCount);

    @Modifying
    @Query("UPDATE BatchJob j SET j.status = :status, " +
            "j.completedAt = :completedAt, " +
            "j.durationMs = :durationMs, " +
            "j.errorSummary = :errorSummary " +
            "WHERE j.id = :jobId")
    int completeJob(@Param("jobId") UUID jobId,
                    @Param("status") JobStatus status,
                    @Param("completedAt") OffsetDateTime completedAt,
                    @Param("durationMs") Long durationMs,
                    @Param("errorSummary") String errorSummary);

    @Modifying
    @Query("UPDATE BatchJob j SET j.status = :status, " +
            "j.startedAt = :startedAt " +
            "WHERE j.id = :jobId AND j.status = :expectedStatus")
    int startJobIfSubmitted(@Param("jobId") UUID jobId,
                            @Param("status") JobStatus status,
                            @Param("startedAt") OffsetDateTime startedAt,
                            @Param("expectedStatus") JobStatus expectedStatus);

    default int startJobIfSubmitted(UUID jobId, JobStatus status, OffsetDateTime startedAt) {
        return startJobIfSubmitted(jobId, status, startedAt, JobStatus.SUBMITTED);
    }
}
