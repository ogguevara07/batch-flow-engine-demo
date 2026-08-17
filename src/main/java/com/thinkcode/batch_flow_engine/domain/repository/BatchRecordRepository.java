package com.thinkcode.batch_flow_engine.domain.repository;

import com.thinkcode.batch_flow_engine.domain.entity.BatchRecord;
import com.thinkcode.batch_flow_engine.domain.enums.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BatchRecordRepository extends JpaRepository<BatchRecord, UUID> {

    Page<BatchRecord> findByBatchJobIdOrderByCreatedAtAsc(UUID batchJobId, Pageable pageable);

    Page<BatchRecord> findByBatchJobIdAndStatusOrderByCreatedAtAsc(UUID batchJobId, RecordStatus status, Pageable pageable);

    List<BatchRecord> findByBatchJobIdAndChunkIndex(UUID batchJobId, int chunkIndex);

    long countByBatchJobIdAndStatus(UUID batchJobId, RecordStatus status);

    long countByBatchJobId(UUID batchJobId);
}
