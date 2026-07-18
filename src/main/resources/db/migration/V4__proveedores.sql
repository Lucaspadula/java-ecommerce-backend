-- Cartera de proveedores por empresa. Mismo criterio de scoping multiempresa
-- que cliente/producto: empresa_id como FK plana. Además de los datos de
-- contacto, se trackea el estado del pedido en curso a ese proveedor.

CREATE TABLE proveedor (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id              BIGINT NOT NULL,
    nombre                  VARCHAR(150) NOT NULL,
    contacto                VARCHAR(190) NULL,
    notas                   TEXT NULL,
    estado_pedido           VARCHAR(20) NOT NULL DEFAULT 'SIN_PEDIDO',
    detalle_pedido_actual   TEXT NULL,
    fecha_pedido            DATE NULL,
    fecha_llegada_estimada  DATE NULL,
    ultimo_pedido_detalle   TEXT NULL,
    activo                  BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_alta              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_proveedor_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT chk_proveedor_estado_pedido
        CHECK (estado_pedido IN ('SIN_PEDIDO', 'EN_PROGRESO', 'RECIBIDO'))
);

CREATE INDEX idx_proveedor_empresa ON proveedor (empresa_id);
