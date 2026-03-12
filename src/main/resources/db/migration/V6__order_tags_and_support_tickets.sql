CREATE TABLE IF NOT EXISTS order_tags (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tag_code VARCHAR(32) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    tag_group VARCHAR(40) NOT NULL,
    tone VARCHAR(24) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_order_tags_tag_code UNIQUE (tag_code)
);

CREATE TABLE IF NOT EXISTS order_tag_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_tag_id BIGINT NOT NULL,
    assigned_by_user_id BIGINT NULL,
    assigned_by_username VARCHAR(50) NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_order_tag_assignments_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_tag_assignments_tag FOREIGN KEY (order_tag_id) REFERENCES order_tags (id),
    CONSTRAINT uk_order_tag_assignments_order_tag UNIQUE (order_id, order_tag_id)
);

CREATE TABLE IF NOT EXISTS support_tickets (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    ticket_no VARCHAR(32) NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    requested_by_username VARCHAR(50) NOT NULL,
    ticket_status VARCHAR(24) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    category VARCHAR(40) NOT NULL,
    subject VARCHAR(140) NOT NULL,
    customer_message TEXT NOT NULL,
    latest_note VARCHAR(500) NULL,
    assigned_team VARCHAR(60) NULL,
    assigned_to_username VARCHAR(50) NULL,
    resolution_note VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    resolved_at DATETIME NULL,
    CONSTRAINT uk_support_tickets_ticket_no UNIQUE (ticket_no),
    CONSTRAINT fk_support_tickets_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_order_tag_assignments_order_id ON order_tag_assignments (order_id);
CREATE INDEX idx_support_tickets_status_priority ON support_tickets (ticket_status, priority);
CREATE INDEX idx_support_tickets_order_id ON support_tickets (order_id);

INSERT INTO order_tags (tag_code, display_name, tag_group, tone, created_at, updated_at)
SELECT 'VIP', 'VIP Customer', 'CUSTOMER', 'emerald', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM order_tags WHERE tag_code = 'VIP');

INSERT INTO order_tags (tag_code, display_name, tag_group, tone, created_at, updated_at)
SELECT 'ADDRESS_CHECK', 'Address Check', 'FULFILLMENT', 'sun', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM order_tags WHERE tag_code = 'ADDRESS_CHECK');

INSERT INTO order_tags (tag_code, display_name, tag_group, tone, created_at, updated_at)
SELECT 'FRAUD_REVIEW', 'Fraud Review', 'RISK', 'accent', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM order_tags WHERE tag_code = 'FRAUD_REVIEW');

INSERT INTO order_tags (tag_code, display_name, tag_group, tone, created_at, updated_at)
SELECT 'WHOLESALE', 'Wholesale Account', 'SALES', 'emerald', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM order_tags WHERE tag_code = 'WHOLESALE');

INSERT INTO order_tags (tag_code, display_name, tag_group, tone, created_at, updated_at)
SELECT 'HIGH_TOUCH', 'High Touch Support', 'SERVICE', 'sun', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM order_tags WHERE tag_code = 'HIGH_TOUCH');
