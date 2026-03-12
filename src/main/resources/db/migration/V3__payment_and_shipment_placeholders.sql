CREATE TABLE payment_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    payment_status VARCHAR(30) NOT NULL,
    transaction_ref VARCHAR(64) NOT NULL,
    provider_code VARCHAR(40) NOT NULL,
    provider_event_id VARCHAR(64) NULL,
    amount DECIMAL(12, 2) NOT NULL,
    note VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_payment_transactions_ref UNIQUE (transaction_ref),
    CONSTRAINT fk_payment_transactions_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_payment_transactions_order_created_at
    ON payment_transactions (order_id, created_at DESC);

CREATE TABLE shipments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    shipment_no VARCHAR(64) NOT NULL,
    carrier_code VARCHAR(40) NOT NULL,
    tracking_no VARCHAR(64) NOT NULL,
    shipment_status VARCHAR(30) NOT NULL,
    status_note VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    shipped_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at DATETIME NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_shipments_no UNIQUE (shipment_no),
    CONSTRAINT uq_shipments_tracking UNIQUE (tracking_no),
    CONSTRAINT fk_shipments_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_shipments_order_created_at
    ON shipments (order_id, created_at DESC);
