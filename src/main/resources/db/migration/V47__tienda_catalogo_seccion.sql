CREATE TABLE tienda_catalogo_seccion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    texto VARCHAR(500) NOT NULL,
    orden INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tienda_catalogo_seccion_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

CREATE INDEX idx_tienda_catalogo_seccion_empresa_tipo ON tienda_catalogo_seccion (empresa_id, tipo);

ALTER TABLE empresa DROP COLUMN tienda_catalogo_politicas;
ALTER TABLE empresa DROP COLUMN tienda_catalogo_como_comprar;
