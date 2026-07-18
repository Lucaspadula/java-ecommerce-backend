-- Flujo unificado de Presupuesto+Venta. Una fila de venta arranca en estado
-- PRESUPUESTO y va avanzando de estado hasta ENTREGADA (o se cancela). Los
-- items snapshotean nombre/precio del producto al momento de la venta: si
-- después se edita o desactiva el producto, el item histórico no cambia.

CREATE TABLE venta (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id     BIGINT NOT NULL,
    cliente_id     BIGINT NOT NULL,
    estado         VARCHAR(20) NOT NULL DEFAULT 'PRESUPUESTO',
    fecha_pedido   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_entrega  DATE NULL,
    notas          TEXT NULL,
    total          DECIMAL(12,2) NOT NULL DEFAULT 0,
    fecha_alta     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_venta_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT fk_venta_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT chk_venta_estado
        CHECK (estado IN ('PRESUPUESTO', 'CONFIRMADA', 'EN_PROCESO', 'LISTA', 'ENTREGADA', 'CANCELADA'))
);

CREATE INDEX idx_venta_empresa ON venta (empresa_id);
CREATE INDEX idx_venta_cliente ON venta (cliente_id);

CREATE TABLE venta_item (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    venta_id          BIGINT          NOT NULL,
    producto_id       BIGINT          NOT NULL,
    producto_nombre   VARCHAR(150)    NOT NULL,
    cantidad          INT             NOT NULL,
    precio_unitario   DECIMAL(12,2)   NOT NULL,
    personalizacion   TEXT            NULL,
    subtotal          DECIMAL(12,2)   NOT NULL,
    CONSTRAINT fk_ventaitem_venta FOREIGN KEY (venta_id) REFERENCES venta (id) ON DELETE CASCADE,
    CONSTRAINT fk_ventaitem_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
);

CREATE INDEX idx_ventaitem_venta ON venta_item (venta_id);
