-- Auditoría de ajustes manuales de stock de producto (correcciones de
-- inventario: rotura, conteo físico, etc.). Independiente del movimiento de
-- stock que ya hacen las ventas (VentaServiceImpl) — esta tabla solo registra
-- los ajustes hechos a mano vía "Ajustar stock".

CREATE TABLE producto_stock_ajuste (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id       BIGINT       NOT NULL,
    empresa_id        BIGINT       NOT NULL,
    delta             INT          NOT NULL,
    stock_resultante  INT          NOT NULL,
    motivo            VARCHAR(255) NOT NULL,
    usuario_id        BIGINT       NOT NULL,
    fecha             DATETIME     NOT NULL,
    CONSTRAINT fk_producto_stock_ajuste_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT fk_producto_stock_ajuste_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

CREATE INDEX idx_producto_stock_ajuste_producto_id ON producto_stock_ajuste (producto_id);
