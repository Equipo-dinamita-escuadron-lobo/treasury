ALTER TABLE supplier_invoice_replicas DROP CONSTRAINT uk_supplier_invoice_enterprise;
ALTER TABLE supplier_invoice_replicas ADD CONSTRAINT uk_supplier_invoice_tenant_enterprise
    UNIQUE (tenant_id, source_invoice_id, enterprise_id);

ALTER TABLE payment_vouchers DROP CONSTRAINT uk_voucher_number_enterprise;
ALTER TABLE payment_vouchers ADD CONSTRAINT uk_voucher_number_tenant_enterprise
    UNIQUE (tenant_id, voucher_number, enterprise_id);
ALTER TABLE payment_vouchers DROP CONSTRAINT uk_voucher_idempotency_enterprise;
ALTER TABLE payment_vouchers ADD CONSTRAINT uk_voucher_idempotency_tenant_enterprise
    UNIQUE (tenant_id, idempotency_key, enterprise_id);

ALTER TABLE outbox_events DROP CONSTRAINT uk_outbox_event_id;
ALTER TABLE outbox_events ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE outbox_events ADD COLUMN next_attempt_at TIMESTAMP WITH TIME ZONE;
UPDATE outbox_events SET status = CASE WHEN published_at IS NULL THEN 'PENDING' ELSE 'PUBLISHED' END,
    next_attempt_at = COALESCE(created_at, now());
ALTER TABLE outbox_events ALTER COLUMN next_attempt_at SET NOT NULL;
ALTER TABLE outbox_events ADD CONSTRAINT uk_outbox_tenant_event UNIQUE (tenant_id, event_id);

ALTER TABLE processed_events DROP CONSTRAINT uk_processed_event_id;
ALTER TABLE processed_events ADD CONSTRAINT uk_processed_tenant_event UNIQUE (tenant_id, event_id);

CREATE UNIQUE INDEX uk_schedule_tenant_voucher ON payment_schedules(tenant_id, voucher_id);
CREATE INDEX ix_outbox_retry ON outbox_events(status, next_attempt_at, attempts);
