CREATE TABLE refund_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    requested_by_username VARCHAR(50) NOT NULL,
    refund_status VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    review_note VARCHAR(500) NULL,
    reviewed_by_user_id BIGINT NULL,
    reviewed_by_username VARCHAR(50) NULL,
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_refund_requests_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_refund_requests_order_requested_at
    ON refund_requests (order_id, requested_at DESC);

CREATE INDEX idx_refund_requests_status_requested_at
    ON refund_requests (refund_status, requested_at DESC);

CREATE INDEX idx_orders_status_created_at
    ON orders (status, created_at DESC);
