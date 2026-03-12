ALTER TABLE payment_transactions
    ADD COLUMN provider_reference VARCHAR(128) NULL AFTER provider_event_id;
