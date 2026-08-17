package com.thinkcode.batch_flow_engine.domain.repository;

import com.thinkcode.batch_flow_engine.domain.entity.BatchRecord;
import com.thinkcode.batch_flow_engine.domain.enums.RecordStatus;
import com.thinkcode.batch_flow_engine.domain.model.BatchRecordUpdateItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class BulkBatchRecordRepositoryImpl implements BulkBatchRecordRepository {

    private static final Logger log = LoggerFactory.getLogger(BulkBatchRecordRepositoryImpl.class);

    private final JdbcTemplate jdbcTemplate;

    public BulkBatchRecordRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public int bulkInsert(List<BatchRecord> records, int batchChunkSize) {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        String sql = "INSERT INTO batch_records " +
                "(id, batch_job_id, chunk_index, external_id, payload, status, retry_count, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        long start = System.currentTimeMillis();
        int chunkSize = batchChunkSize > 0 ? batchChunkSize : 500;

        int[][] result = jdbcTemplate.batchUpdate(sql, records, chunkSize, (PreparedStatement ps, BatchRecord record) -> {
            UUID recordId = record.getId() != null ? record.getId() : UUID.randomUUID();
            record.setId(recordId);

            ps.setObject(1, recordId);
            ps.setObject(2, record.getBatchJobId());
            ps.setInt(3, record.getChunkIndex());
            ps.setString(4, record.getExternalId());
            ps.setString(5, record.getPayload());
            ps.setString(6, record.getStatus() != null ? record.getStatus().name() : RecordStatus.PENDING.name());
            ps.setInt(7, record.getRetryCount());

            Timestamp now = new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(8, now);
            ps.setTimestamp(9, now);
        });

        int totalInserted = 0;
        for (int[] chunk : result) {
            for (int count : chunk) {
                if (count >= 0) {
                    totalInserted += count;
                } else if (count == PreparedStatement.SUCCESS_NO_INFO) {
                    totalInserted++;
                }
            }
        }

        long duration = System.currentTimeMillis() - start;
        log.debug("Bulk inserted {} records in {} ms across {} batches", records.size(), duration, result.length);
        return records.size();
    }

    @Override
    @Transactional
    public int bulkUpdateStatus(List<UUID> recordIds, RecordStatus status, OffsetDateTime processedAt, String errorMessage, int retryCount) {
        if (recordIds == null || recordIds.isEmpty()) {
            return 0;
        }

        String sql = "UPDATE batch_records SET " +
                "status = ?, " +
                "processed_at = ?, " +
                "error_message = ?, " +
                "retry_count = ?, " +
                "updated_at = ? " +
                "WHERE id = ?";

        Timestamp processedTs = processedAt != null ? Timestamp.from(processedAt.toInstant()) : new Timestamp(System.currentTimeMillis());
        Timestamp now = new Timestamp(System.currentTimeMillis());

        int[][] result = jdbcTemplate.batchUpdate(sql, recordIds, 500, (PreparedStatement ps, UUID recordId) -> {
            ps.setString(1, status.name());
            ps.setTimestamp(2, processedTs);
            ps.setString(3, errorMessage);
            ps.setInt(4, retryCount);
            ps.setTimestamp(5, now);
            ps.setObject(6, recordId);
        });

        return recordIds.size();
    }

    @Override
    @Transactional
    public int bulkUpdateOutcomes(List<BatchRecordUpdateItem> updates) {
        if (updates == null || updates.isEmpty()) {
            return 0;
        }

        String sql = "UPDATE batch_records SET " +
                "status = ?, " +
                "processed_at = ?, " +
                "error_message = ?, " +
                "retry_count = ?, " +
                "updated_at = ? " +
                "WHERE id = ?";

        Timestamp now = new Timestamp(System.currentTimeMillis());

        jdbcTemplate.batchUpdate(sql, updates, 500, (PreparedStatement ps, BatchRecordUpdateItem item) -> {
            ps.setString(1, item.getStatus().name());
            Timestamp processedTs = item.getProcessedAt() != null
                    ? Timestamp.from(item.getProcessedAt().toInstant())
                    : now;
            ps.setTimestamp(2, processedTs);
            ps.setString(3, item.getErrorMessage());
            ps.setInt(4, item.getRetryCount());
            ps.setTimestamp(5, now);
            ps.setObject(6, item.getRecordId());
        });

        return updates.size();
    }
}
