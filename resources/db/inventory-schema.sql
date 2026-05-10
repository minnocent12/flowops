-- inventory-schema.sql
CREATE TABLE IF NOT EXISTS inventory (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id VARCHAR(100) NOT NULL,
    sku          VARCHAR(100) NOT NULL,
    quantity     INT NOT NULL CHECK (quantity >= 0),
    UNIQUE (warehouse_id, sku)
);

-- Seed some warehouses and stock
INSERT INTO inventory (warehouse_id, sku, quantity) VALUES
    ('WH-ATLANTA',  'SKU-001', 150),
    ('WH-ATLANTA',  'SKU-002', 80),
    ('WH-DALLAS',   'SKU-001', 200),
    ('WH-DALLAS',   'SKU-002', 0),
    ('WH-CHICAGO',  'SKU-001', 50),
    ('WH-CHICAGO',  'SKU-002', 120)
ON CONFLICT DO NOTHING;
