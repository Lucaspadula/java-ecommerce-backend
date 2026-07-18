CREATE TABLE venta_estado_historial (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venta_id BIGINT NOT NULL,
    estado_anterior VARCHAR(20) NOT NULL,
    estado_nuevo VARCHAR(20) NOT NULL,
    usuario_id BIGINT NOT NULL,
    fecha DATETIME NOT NULL,
    CONSTRAINT fk_venta_estado_historial_venta FOREIGN KEY (venta_id) REFERENCES venta(id)
);

CREATE INDEX idx_venta_estado_historial_venta_id ON venta_estado_historial (venta_id);
