package com.thinkcode.batch_flow_engine.domain.repository;

import com.thinkcode.batch_flow_engine.domain.entity.BatchRecord;
import com.thinkcode.batch_flow_engine.domain.enums.RecordStatus;
import com.thinkcode.batch_flow_engine.domain.model.BatchRecordUpdateItem;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BulkBatchRecordRepository {

    /**
     * Executes high-speed batch insert into batch_records table using native JDBC batch updates.
     *
     * @param records List of BatchRecord entities to insert
     * @param batchChunkSize Size of each SQL batch statement
     * @return Total records inserted
     */
    int bulkInsert(List<BatchRecord> records, int batchChunkSize);

    /**
     * Updates status and timestamps for a collection of records in batch.
     */
    int bulkUpdateStatus(List<UUID> recordIds, RecordStatus status, OffsetDateTime processedAt, String errorMessage, int retryCount);

    /**
     * Executes bulk updates with individual record statuses and error messages.
     */
    int bulkUpdateOutcomes(List<BatchRecordUpdateItem> updates);
}
