ALTER TABLE orders
    ADD COLUMN shipping_receiver_name VARCHAR(80) NULL AFTER total_amount,
    ADD COLUMN shipping_phone VARCHAR(25) NULL AFTER shipping_receiver_name,
    ADD COLUMN shipping_line1 VARCHAR(120) NULL AFTER shipping_phone,
    ADD COLUMN shipping_line2 VARCHAR(120) NULL AFTER shipping_line1,
    ADD COLUMN shipping_city VARCHAR(80) NULL AFTER shipping_line2,
    ADD COLUMN shipping_province VARCHAR(80) NULL AFTER shipping_city,
    ADD COLUMN shipping_postal_code VARCHAR(20) NULL AFTER shipping_province;

CREATE TABLE customer_addresses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    address_label VARCHAR(40) NOT NULL,
    receiver_name VARCHAR(80) NOT NULL,
    phone VARCHAR(25) NOT NULL,
    line1 VARCHAR(120) NOT NULL,
    line2 VARCHAR(120) NULL,
    city VARCHAR(80) NOT NULL,
    province VARCHAR(80) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    is_default BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_customer_addresses_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_customer_addresses_user_default
    ON customer_addresses (user_id, is_default, created_at DESC);

CREATE TABLE order_internal_notes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    note_text VARCHAR(500) NOT NULL,
    created_by_user_id BIGINT NULL,
    created_by_username VARCHAR(50) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_internal_notes_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_order_internal_notes_order_created_at
    ON order_internal_notes (order_id, created_at DESC);
