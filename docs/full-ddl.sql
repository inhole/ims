-- IMS Inventory Management System
-- PostgreSQL full DDL for assignment submission

DROP TABLE IF EXISTS stock_movements;
DROP TABLE IF EXISTS products;

CREATE TABLE products (
    id BIGSERIAL,
    name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uk_products_name UNIQUE (name),
    CONSTRAINT chk_products_quantity_non_negative CHECK (quantity >= 0)
);

CREATE INDEX idx_products_name ON products (name);

COMMENT ON TABLE products IS '상품';
COMMENT ON COLUMN products.id IS '상품 고유 식별자';
COMMENT ON COLUMN products.name IS '상품명';
COMMENT ON COLUMN products.quantity IS '현재 재고 수량';
COMMENT ON COLUMN products.created_at IS '생성 일시';
COMMENT ON COLUMN products.updated_at IS '수정 일시';

CREATE TABLE stock_movements (
    id BIGSERIAL,
    product_id BIGINT NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_stock_movements PRIMARY KEY (id),
    CONSTRAINT fk_stock_movements_product
        FOREIGN KEY (product_id)
        REFERENCES products (id),
    CONSTRAINT chk_stock_movements_type
        CHECK (movement_type IN ('INBOUND', 'OUTBOUND')),
    CONSTRAINT chk_stock_movements_quantity_positive
        CHECK (quantity > 0)
);

CREATE INDEX idx_stock_movements_product_id ON stock_movements (product_id);
CREATE INDEX idx_stock_movements_created_at ON stock_movements (created_at);

COMMENT ON TABLE stock_movements IS '재고 변경 이력';
COMMENT ON COLUMN stock_movements.id IS '재고 변경 이력 고유 식별자';
COMMENT ON COLUMN stock_movements.product_id IS '상품 고유 식별자';
COMMENT ON COLUMN stock_movements.movement_type IS '재고 변경 유형(INBOUND, OUTBOUND)';
COMMENT ON COLUMN stock_movements.quantity IS '재고 변경 수량';
COMMENT ON COLUMN stock_movements.created_at IS '생성 일시';
