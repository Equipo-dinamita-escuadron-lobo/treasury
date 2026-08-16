ALTER TABLE payment_schedule_details ADD COLUMN canceled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE payment_schedule_details ADD COLUMN cancellation_reason VARCHAR(500);
