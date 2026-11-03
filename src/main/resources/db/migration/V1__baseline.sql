-- V1: baseline schema for the MarketHub Order & Search Service.
-- Six tables, exactly as in section 4 of the project guide.
-- Money columns are DECIMAL(12,3) because the platform trades in KWD (3 decimal places).

CREATE TABLE customer (
    id      BIGINT       NOT NULL AUTO_INCREMENT,
    email   VARCHAR(255) NOT NULL,
    tier    VARCHAR(16)  NOT NULL,
    country CHAR(2)      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (email)
);

CREATE TABLE product (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    sku          VARCHAR(64)   NOT NULL,
    name_en      VARCHAR(255)  NOT NULL,
    name_ar      VARCHAR(255)  NOT NULL,
    brand        VARCHAR(64)   NOT NULL,
    category     VARCHAR(64)   NOT NULL,
    price_amount DECIMAL(12,3) NOT NULL,
    currency     CHAR(3)       NOT NULL,
    status       VARCHAR(16)   NOT NULL,
    version      BIGINT        NOT NULL DEFAULT 0,
    updated_at   DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (sku)
);

CREATE TABLE orders (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    order_number    VARCHAR(32)   NOT NULL,
    customer_id     BIGINT        NOT NULL,
    idempotency_key VARCHAR(64)   NOT NULL,
    request_hash    CHAR(64)      NOT NULL,
    status          VARCHAR(16)   NOT NULL,
    coupon_code     VARCHAR(32)   NULL,
    subtotal        DECIMAL(12,3) NOT NULL,
    discount        DECIMAL(12,3) NOT NULL,
    shipping        DECIMAL(12,3) NOT NULL,
    total           DECIMAL(12,3) NOT NULL,
    promotions      JSON          NOT NULL,
    ship_country    CHAR(2)       NOT NULL,
    ship_city       VARCHAR(100)  NOT NULL,
    ship_line1      VARCHAR(255)  NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0,
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (order_number),
    UNIQUE (idempotency_key),
    FOREIGN KEY (customer_id) REFERENCES customer (id)
);

-- Order history: orders of one customer, newest first.
CREATE INDEX idx_orders_customer_created ON orders (customer_id, created_at);

CREATE TABLE order_line (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    order_id   BIGINT        NOT NULL,
    sku        VARCHAR(64)   NOT NULL,
    name       VARCHAR(255)  NOT NULL,
    quantity   INT           NOT NULL,
    unit_price DECIMAL(12,3) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (order_id) REFERENCES orders (id)
);

-- SINGLE_TABLE inheritance: card_last4 and auth_code belong to CARD payments,
-- wallet_txn_id to WALLET payments; they stay NULL for the other kinds.
CREATE TABLE payment (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    order_id      BIGINT        NOT NULL,
    method        VARCHAR(16)   NOT NULL,
    amount        DECIMAL(12,3) NOT NULL,
    currency      CHAR(3)       NOT NULL,
    status        VARCHAR(16)   NOT NULL,
    card_last4    CHAR(4)       NULL,
    auth_code     VARCHAR(32)   NULL,
    wallet_txn_id VARCHAR(64)   NULL,
    PRIMARY KEY (id),
    UNIQUE (order_id),
    FOREIGN KEY (order_id) REFERENCES orders (id)
);

-- Transactional outbox (used from M5). id is the event id.
CREATE TABLE outbox_event (
    id           CHAR(36)     NOT NULL,
    aggregate_id VARCHAR(64)  NOT NULL,
    event_type   VARCHAR(64)  NOT NULL,
    topic        VARCHAR(128) NOT NULL,
    payload      JSON         NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    published_at DATETIME(6)  NULL,
    PRIMARY KEY (id)
);

-- The relay reads unpublished rows, oldest first.
CREATE INDEX idx_outbox_unpublished ON outbox_event (published_at, created_at);
