-- ==============================================================================
-- Schema: Batch Flow Engine
-- Description: Optimized tables and indexes for high-throughput batch processing
-- ==============================================================================

-- Create extension for UUID generation if not exists
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Table: batch_jobs
CREATE TABLE IF NOT EXISTS batch_jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_records BIGINT NOT NULL DEFAULT 0,
    processed_records BIGINT NOT NULL DEFAULT 0,
    failed_records BIGINT NOT NULL DEFAULT 0,
    chunk_size INT NOT NULL DEFAULT 100,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    error_summary TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index on batch_jobs status and creation date
CREATE INDEX IF NOT EXISTS idx_batch_jobs_status ON batch_jobs(status);
CREATE INDEX IF NOT EXISTS idx_batch_jobs_created_at ON batch_jobs(created_at DESC);

-- 2. Table: batch_records
CREATE TABLE IF NOT EXISTS batch_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    batch_job_id UUID NOT NULL REFERENCES batch_jobs(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL DEFAULT 0,
    external_id VARCHAR(120),
    payload JSONB NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes optimized for batch querying, worker polling, and status reporting
CREATE INDEX IF NOT EXISTS idx_batch_records_job_status ON batch_records(batch_job_id, status);
CREATE INDEX IF NOT EXISTS idx_batch_records_status_created ON batch_records(status, created_at);
CREATE INDEX IF NOT EXISTS idx_batch_records_external_id ON batch_records(external_id);
CREATE INDEX IF NOT EXISTS idx_batch_records_job_chunk ON batch_records(batch_job_id, chunk_index);
CREATE INDEX IF NOT EXISTS idx_batch_records_payload_gin ON batch_records USING GIN (payload);

-- 3. Table: dead_letter_records
CREATE TABLE IF NOT EXISTS dead_letter_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    batch_job_id UUID REFERENCES batch_jobs(id) ON DELETE SET NULL,
    record_id UUID,
    chunk_index INT,
    payload JSONB NOT NULL,
    exception_class VARCHAR(255),
    error_message TEXT,
    stack_trace TEXT,
    retry_attempts INT NOT NULL DEFAULT 0,
    original_queue VARCHAR(100),
    original_exchange VARCHAR(100),
    routing_key VARCHAR(100),
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolution_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for DLQ inspection and re-queuing
CREATE INDEX IF NOT EXISTS idx_dlq_job_id ON dead_letter_records(batch_job_id);
CREATE INDEX IF NOT EXISTS idx_dlq_resolved ON dead_letter_records(resolved, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_dlq_created_at ON dead_letter_records(created_at DESC);
