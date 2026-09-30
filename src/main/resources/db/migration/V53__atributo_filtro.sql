-- Atributos personalizados de filtro, definidos por el dueño POR CATEGORÍA
-- (ej. categoría "Sartenes" -> atributo "Material" -> valores "Acero",
-- "Hierro fundido") — la pantalla pública de categoría arma sus chips de
-- filtro a partir de esto, en vez de un set fijo igual para todo el catálogo
-- (pedido explícito: "depende mucho de qué producto").
CREATE TABLE atributo_filtro (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    empresa_id   BIGINT       NOT NULL,
    categoria_id BIGINT       NOT NULL,
    nombre       VARCHAR(100) NOT NULL,
    orden        INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_atributo_filtro_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id)
);
CREATE INDEX idx_atributo_filtro_categoria_id ON atributo_filtro (categoria_id);

-- Valores posibles de cada atributo (ej. "Acero", "Hierro fundido").
CREATE TABLE atributo_filtro_valor (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    atributo_filtro_id BIGINT      NOT NULL,
    valor             VARCHAR(100) NOT NULL,
    orden             INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_atributo_filtro_valor_atributo FOREIGN KEY (atributo_filtro_id) REFERENCES atributo_filtro (id)
);
CREATE INDEX idx_atributo_filtro_valor_atributo_id ON atributo_filtro_valor (atributo_filtro_id);

-- Asignación real: qué valor tiene ESTE producto para ESE atributo (ej.
-- Producto "Sartén 28cm" -> atributo "Material" -> valor "Acero"). A lo
-- sumo un valor por (producto, atributo) — se valida en el servicio, no acá
-- (MySQL/MariaDB no valida "un solo valor por atributo" con una unique
-- simple sobre producto_id porque un producto puede tener valores de VARIOS
-- atributos distintos a la vez).
CREATE TABLE producto_atributo_valor (
    producto_id          BIGINT NOT NULL,
    atributo_filtro_valor_id BIGINT NOT NULL,
    PRIMARY KEY (producto_id, atributo_filtro_valor_id),
    CONSTRAINT fk_pav_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT fk_pav_valor FOREIGN KEY (atributo_filtro_valor_id) REFERENCES atributo_filtro_valor (id)
);
