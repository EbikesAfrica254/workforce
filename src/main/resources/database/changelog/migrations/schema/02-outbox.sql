--comment: create an outbox table for transactional event publication with an at-least-once delivery guarantee
CREATE TABLE workforce.outbox (
   id UUID NOT NULL DEFAULT gen_random_uuid(),
   created_at TIMESTAMPTZ NOT NULL,
   event_type VARCHAR(100) NOT NULL,
   payload JSONB NOT NULL,
   status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
   retry_count INTEGER NOT NULL DEFAULT 0,
   routing_key VARCHAR(200),
   updated_at TIMESTAMPTZ,
   CONSTRAINT pk_outbox PRIMARY KEY (id)
);

--comment: add check constraints for the outbox table
ALTER TABLE workforce.outbox
    ADD CONSTRAINT chk_outbox_status
        CHECK (status IN ('DEAD_LETTER', 'FAILED', 'PENDING', 'SENT'));

ALTER TABLE workforce.outbox
    ADD CONSTRAINT chk_outbox_retry_count
        CHECK (retry_count >= 0);

--comment: create indexes for outbox table
CREATE INDEX idx_outbox_created_at ON workforce.outbox(created_at);
CREATE INDEX idx_outbox_pending ON workforce.outbox(status) WHERE status = 'PENDING';

--comment: add table and column comments for outbox
COMMENT ON TABLE workforce.outbox IS 'Transactional outbox pattern - written atomically with domain state changes, polled by relay process for publication to RabbitMQ';
COMMENT ON COLUMN workforce.outbox.id IS 'Primary key - auto-incrementing bigint for sequential polling';
COMMENT ON COLUMN workforce.outbox.created_at IS 'Timestamp when event was written to outbox';
COMMENT ON COLUMN workforce.outbox.event_type IS 'Domain event type name - maps to routing key on RabbitMQ topic exchange';
COMMENT ON COLUMN workforce.outbox.payload IS 'Full event payload as JSON - contains all data needed by consumers';
COMMENT ON COLUMN workforce.outbox.status IS 'Publication state - PENDING awaits relay, SENT confirms published, FAILED after max retries';
COMMENT ON COLUMN workforce.outbox.retry_count IS 'Number of publication attempts - incremented on transient failures';
COMMENT ON COLUMN workforce.outbox.routing_key IS 'Optional routing key for RabbitMQ topic exchange';
COMMENT ON COLUMN workforce.outbox.updated_at IS 'Timestamp of last status update';
