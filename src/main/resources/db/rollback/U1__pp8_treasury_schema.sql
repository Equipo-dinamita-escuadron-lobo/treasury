-- Manual rollback for non-production environments. Flyway Community does not execute undo migrations.
DROP TABLE IF EXISTS due_date_history;
DROP TABLE IF EXISTS processed_events;
DROP TABLE IF EXISTS outbox_events;
DROP TABLE IF EXISTS payable_write_off_details;
DROP TABLE IF EXISTS payable_write_offs;
DROP TABLE IF EXISTS payment_schedule_details;
DROP TABLE IF EXISTS payment_schedules;
DROP TABLE IF EXISTS payment_voucher_details;
DROP TABLE IF EXISTS payment_vouchers;
DROP TABLE IF EXISTS supplier_invoice_replicas;
