-- Convierte Insumo de texto suelto embebido en cada Producto a una entidad
-- maestra real por empresa, con stock propio. producto_insumo pasa a ser una
-- "receta": referencia a un Insumo maestro + cantidad usada por unidad de
-- producto, en vez de repetir nombre/costo en cada línea.

CREATE TABLE insumo (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id      BIGINT          NOT NULL,
    nombre          VARCHAR(150)    NOT NULL,
    costo_unitario  DECIMAL(12,2)   NOT NULL,
    stock           DECIMAL(12,3)   NOT NULL DEFAULT 0,
    stock_minimo    DECIMAL(12,3)   NULL,
    unidad_medida   VARCHAR(30)     NULL,
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_alta      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_insumo_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

CREATE INDEX idx_insumo_empresa ON insumo (empresa_id);

-- Migra cada combinación única (empresa, nombre de insumo) ya usada en
-- productos a un Insumo maestro, con el costo más alto visto para ese nombre
-- (si hay inconsistencias entre productos, es una elección razonable — no hay
-- forma perfecta de reconciliar costos históricos distintos bajo el mismo
-- nombre).
INSERT INTO insumo (empresa_id, nombre, costo_unitario, stock, activo, fecha_alta)
SELECT p.empresa_id, pi.nombre, MAX(pi.costo), 0, TRUE, CURRENT_TIMESTAMP
FROM producto_insumo pi
JOIN producto p ON p.id = pi.producto_id
GROUP BY p.empresa_id, pi.nombre;

ALTER TABLE producto_insumo ADD COLUMN insumo_id BIGINT NULL;
ALTER TABLE producto_insumo ADD COLUMN cantidad DECIMAL(12,3) NOT NULL DEFAULT 1;

UPDATE producto_insumo pi
JOIN producto p ON p.id = pi.producto_id
JOIN insumo i ON i.empresa_id = p.empresa_id AND i.nombre = pi.nombre
SET pi.insumo_id = i.id;

ALTER TABLE producto_insumo MODIFY COLUMN insumo_id BIGINT NOT NULL;
ALTER TABLE producto_insumo ADD CONSTRAINT fk_productoinsumo_insumo FOREIGN KEY (insumo_id) REFERENCES insumo (id);
ALTER TABLE producto_insumo DROP COLUMN nombre;
ALTER TABLE producto_insumo DROP COLUMN costo;
