-- Catálogo de productos por empresa. Cada producto tiene una receta de
-- insumos (materiales que lo componen); el costo/margen se calcula en el
-- frontend, acá solo se persisten los datos crudos.

CREATE TABLE producto (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id     BIGINT          NOT NULL,
    nombre         VARCHAR(150)    NOT NULL,
    categoria      VARCHAR(80)     NOT NULL,
    subcategoria   VARCHAR(80)     NULL,
    descripcion    TEXT            NULL,
    precio_venta   DECIMAL(12,2)   NOT NULL,
    foto_url       VARCHAR(255)    NULL,
    activo         BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_alta     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_producto_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

CREATE INDEX idx_producto_empresa ON producto (empresa_id);

CREATE TABLE producto_insumo (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id   BIGINT          NOT NULL,
    nombre        VARCHAR(150)    NOT NULL,
    costo         DECIMAL(12,2)   NOT NULL,
    CONSTRAINT fk_insumo_producto FOREIGN KEY (producto_id) REFERENCES producto (id) ON DELETE CASCADE
);

CREATE INDEX idx_insumo_producto ON producto_insumo (producto_id);
