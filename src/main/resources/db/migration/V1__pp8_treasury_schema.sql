CREATE TABLE supplier_invoice_replicas (
    id BIGSERIAL PRIMARY KEY, source_invoice_id BIGINT NOT NULL, reference VARCHAR(80) NOT NULL,
    enterprise_id VARCHAR(80) NOT NULL, supplier_id BIGINT NOT NULL,
    original_amount NUMERIC(19,2) NOT NULL, paid_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    pending_amount NUMERIC(19,2) NOT NULL, reserved_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    issue_date DATE NOT NULL, original_due_date DATE NOT NULL, due_date DATE NOT NULL,
    due_date_overridden BOOLEAN NOT NULL DEFAULT FALSE, payable_account_id BIGINT NOT NULL,
    payable_account_code VARCHAR(30) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
    last_event_id VARCHAR(80), updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0, tenant_id VARCHAR(80) NOT NULL,
    CONSTRAINT uk_supplier_invoice_enterprise UNIQUE(source_invoice_id, enterprise_id),
    CONSTRAINT ck_supplier_invoice_amounts CHECK (original_amount >= 0 AND paid_amount >= 0 AND pending_amount >= 0 AND reserved_amount >= 0 AND reserved_amount <= pending_amount)
);

CREATE TABLE payment_vouchers (
    id BIGSERIAL PRIMARY KEY, voucher_number VARCHAR(50) NOT NULL, enterprise_id VARCHAR(80) NOT NULL,
    issue_date DATE NOT NULL, status VARCHAR(20) NOT NULL, payment_method_id BIGINT NOT NULL,
    bank_account_id BIGINT, total NUMERIC(19,2) NOT NULL, observations VARCHAR(500),
    idempotency_key VARCHAR(100), accounting_entry_id BIGINT, failure_reason VARCHAR(500), void_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0, tenant_id VARCHAR(80) NOT NULL,
    CONSTRAINT uk_voucher_number_enterprise UNIQUE(voucher_number,enterprise_id),
    CONSTRAINT uk_voucher_idempotency_enterprise UNIQUE(idempotency_key,enterprise_id),
    CONSTRAINT ck_voucher_total CHECK(total > 0)
);

CREATE TABLE payment_voucher_details (
    id BIGSERIAL PRIMARY KEY, voucher_id BIGINT NOT NULL REFERENCES payment_vouchers(id) ON DELETE CASCADE,
    supplier_id BIGINT NOT NULL, invoice_id BIGINT NOT NULL, invoice_reference VARCHAR(80) NOT NULL,
    payable_account_id BIGINT NOT NULL, payable_account_code VARCHAR(30) NOT NULL,
    previous_balance NUMERIC(19,2) NOT NULL, amount_paid NUMERIC(19,2) NOT NULL,
    remaining_balance NUMERIC(19,2) NOT NULL, tenant_id VARCHAR(80) NOT NULL,
    CONSTRAINT uk_voucher_invoice UNIQUE(voucher_id,invoice_id),
    CONSTRAINT ck_voucher_detail_amount CHECK(amount_paid > 0 AND remaining_balance >= 0)
);

CREATE TABLE payment_schedules (
    id BIGSERIAL PRIMARY KEY, enterprise_id VARCHAR(80) NOT NULL, execution_date DATE NOT NULL,
    payment_method_id BIGINT NOT NULL, bank_account_id BIGINT, observations VARCHAR(500),
    status VARCHAR(20) NOT NULL, voucher_id BIGINT, retry_count INTEGER NOT NULL DEFAULT 0,
    failure_reason VARCHAR(500), created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    tenant_id VARCHAR(80) NOT NULL
);
CREATE TABLE payment_schedule_details (
    id BIGSERIAL PRIMARY KEY, schedule_id BIGINT NOT NULL REFERENCES payment_schedules(id) ON DELETE CASCADE,
    supplier_id BIGINT NOT NULL, invoice_id BIGINT NOT NULL, amount NUMERIC(19,2) NOT NULL,
    tenant_id VARCHAR(80) NOT NULL, CONSTRAINT uk_schedule_invoice UNIQUE(schedule_id,invoice_id),
    CONSTRAINT ck_schedule_amount CHECK(amount > 0)
);

CREATE TABLE payable_write_offs (
    id BIGSERIAL PRIMARY KEY, enterprise_id VARCHAR(80) NOT NULL, reason VARCHAR(500) NOT NULL,
    counterpart_account_id BIGINT NOT NULL, counterpart_account_code VARCHAR(30) NOT NULL,
    total NUMERIC(19,2) NOT NULL, status VARCHAR(20) NOT NULL, accounting_entry_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0, tenant_id VARCHAR(80) NOT NULL,
    CONSTRAINT ck_write_off_total CHECK(total > 0)
);
CREATE TABLE payable_write_off_details (
    id BIGSERIAL PRIMARY KEY, write_off_id BIGINT NOT NULL REFERENCES payable_write_offs(id) ON DELETE CASCADE,
    supplier_id BIGINT NOT NULL, invoice_id BIGINT NOT NULL, payable_account_id BIGINT NOT NULL,
    payable_account_code VARCHAR(30) NOT NULL, amount NUMERIC(19,2) NOT NULL, tenant_id VARCHAR(80) NOT NULL,
    CONSTRAINT ck_write_off_amount CHECK(amount > 0)
);

CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY, event_id VARCHAR(80) NOT NULL, aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL, event_type VARCHAR(80) NOT NULL, payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL, published_at TIMESTAMP WITH TIME ZONE,
    attempts INTEGER NOT NULL DEFAULT 0, last_error VARCHAR(500), tenant_id VARCHAR(80) NOT NULL,
    CONSTRAINT uk_outbox_event_id UNIQUE(event_id)
);
CREATE TABLE processed_events (
    id BIGSERIAL PRIMARY KEY, event_id VARCHAR(80) NOT NULL, processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    tenant_id VARCHAR(80) NOT NULL, CONSTRAINT uk_processed_event_id UNIQUE(event_id)
);
CREATE TABLE due_date_history (
    id BIGSERIAL PRIMARY KEY, invoice_id BIGINT NOT NULL REFERENCES supplier_invoice_replicas(id),
    previous_date DATE NOT NULL, new_date DATE NOT NULL, reason VARCHAR(500) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL, changed_by VARCHAR(100) NOT NULL, tenant_id VARCHAR(80) NOT NULL
);

CREATE INDEX ix_invoice_pending ON supplier_invoice_replicas(enterprise_id,supplier_id,pending_amount);
CREATE INDEX ix_voucher_search ON payment_vouchers(enterprise_id,status,issue_date);
CREATE INDEX ix_schedule_due ON payment_schedules(status,execution_date);
CREATE INDEX ix_outbox_unpublished ON outbox_events(published_at,created_at);
