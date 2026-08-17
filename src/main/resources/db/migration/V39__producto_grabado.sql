-- Lugares grabables por producto (ej. "Virola", "Cuerpo de algarrobo"), cada
-- uno con su propio precio de servicio — no todo producto admite grabado, y
-- los que sí pueden tener más de un lugar distinto con precios distintos.
CREATE TABLE producto_grabado (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    lugar VARCHAR(60) NOT NULL,
    precio DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_producto_grabado_producto FOREIGN KEY (producto_id) REFERENCES producto(id)
);

CREATE INDEX idx_producto_grabado_producto_id ON producto_grabado (producto_id);
