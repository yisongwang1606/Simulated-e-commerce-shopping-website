ALTER TABLE orders
    ADD COLUMN status_note VARCHAR(255) NULL AFTER status,
    ADD COLUMN status_updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER status_note,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER created_at;

CREATE TABLE inventory_adjustments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    adjustment_type VARCHAR(30) NOT NULL,
    quantity_delta INT NOT NULL,
    previous_stock INT NOT NULL,
    new_stock INT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    reference_type VARCHAR(40) NULL,
    reference_id VARCHAR(64) NULL,
    created_by_user_id BIGINT NULL,
    created_by_username VARCHAR(50) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_inventory_adjustments_product
        FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_inventory_adjustments_product_created_at
    ON inventory_adjustments (product_id, created_at DESC);

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT NULL,
    actor_username VARCHAR(50) NULL,
    action_type VARCHAR(60) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    summary VARCHAR(255) NOT NULL,
    details_json TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_audit_logs_created_at
    ON audit_logs (created_at DESC);

CREATE INDEX idx_audit_logs_entity_created_at
    ON audit_logs (entity_type, entity_id, created_at DESC);
