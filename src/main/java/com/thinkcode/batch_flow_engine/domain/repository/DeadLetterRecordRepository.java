package com.thinkcode.batch_flow_engine.domain.repository;

import com.thinkcode.batch_flow_engine.domain.entity.DeadLetterRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeadLetterRecordRepository extends JpaRepository<DeadLetterRecord, UUID> {

    Page<DeadLetterRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<DeadLetterRecord> findByResolvedOrderByCreatedAtDesc(boolean resolved, Pageable pageable);

    List<DeadLetterRecord> findByBatchJobIdAndResolved(UUID batchJobId, boolean resolved);

    long countByResolved(boolean resolved);
}
