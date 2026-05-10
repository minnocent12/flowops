-- shipment-schema.sql
CREATE TABLE IF NOT EXISTS shipments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     VARCHAR(100) NOT NULL UNIQUE,
    warehouse_id VARCHAR(100) NOT NULL,
    carrier      VARCHAR(50)  NOT NULL,
    tracking_id  VARCHAR(100) NOT NULL UNIQUE,
    status       VARCHAR(50)  NOT NULL DEFAULT 'CREATED',
    created_at   TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_shipments_warehouse ON shipments(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_shipments_carrier   ON shipments(carrier);
