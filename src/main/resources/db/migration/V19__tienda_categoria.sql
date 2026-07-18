-- Metadata visual (color + imagen) configurable por categoría de producto,
-- para el carrusel "Explorá por categoría" de la tienda pública. La
-- categoría es texto libre en producto.categoria (no hay tabla maestra), así
-- que esta tabla se relaciona por nombre de texto, no por FK a producto.

CREATE TABLE tienda_categoria (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id  BIGINT       NOT NULL,
    nombre      VARCHAR(100) NOT NULL,
    color       VARCHAR(7)   NULL,
    imagen_url  VARCHAR(500) NULL,
    CONSTRAINT fk_tiendacategoria_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT uk_tiendacategoria_empresa_nombre UNIQUE (empresa_id, nombre)
);

CREATE INDEX idx_tiendacategoria_empresa ON tienda_categoria (empresa_id);
