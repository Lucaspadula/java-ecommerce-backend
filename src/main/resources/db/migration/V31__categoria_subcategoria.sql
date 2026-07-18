-- Convierte categoria/subcategoria de texto libre embebido en Producto (y
-- duplicado, también como texto libre, en regla_descuento_combo y
-- tienda_categoria) a un catálogo maestro real por empresa. Mismo patrón que
-- V10__insumo_maestro.sql: crear la tabla maestra, migrar los datos ya
-- existentes con SQL puro, backfillear las FKs nuevas, y recién al final
-- dropear las columnas de texto viejas.
--
-- El seed de `categoria`/`subcategoria` toma la UNIÓN de las 3 fuentes de
-- texto libre que existían (producto, los dos lados de
-- regla_descuento_combo, y tienda_categoria) — no alcanza con mirar solo
-- Producto: una regla de descuento combo o una config de tienda_categoria
-- podrían referenciar un nombre de categoría que ya no tiene ningún producto
-- activo, y perderíamos esa fila si solo migráramos desde producto.

CREATE TABLE categoria (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id  BIGINT       NOT NULL,
    nombre      VARCHAR(100) NOT NULL,
    orden       INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_categoria_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    -- Unique simple (no un COLLATE explícito distinto): las collation
    -- utf8mb4_*_ci por defecto de MariaDB ya son case-insensitive, así que
    -- "Mates" y "mates" ya chocan acá sin configuración extra.
    CONSTRAINT uk_categoria_empresa_nombre UNIQUE (empresa_id, nombre)
);

CREATE INDEX idx_categoria_empresa ON categoria (empresa_id);

CREATE TABLE subcategoria (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria_id  BIGINT       NOT NULL,
    nombre        VARCHAR(100) NOT NULL,
    CONSTRAINT fk_subcategoria_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT uk_subcategoria_categoria_nombre UNIQUE (categoria_id, nombre)
);

CREATE INDEX idx_subcategoria_categoria ON subcategoria (categoria_id);

-- Semilla de categoria: un nombre por (empresa, nombre normalizado a
-- minúscula), tomando MIN() como representante canónico entre variantes de
-- mayúsculas/espacios ("Mates" / " mates " -> una sola fila "Mates" o
-- "mates" según orden alfabético, no importa cuál gane).
INSERT INTO categoria (empresa_id, nombre)
SELECT empresa_id, MIN(nombre_trim) AS nombre
FROM (
    SELECT empresa_id, TRIM(categoria) AS nombre_trim
    FROM producto
    WHERE categoria IS NOT NULL AND TRIM(categoria) <> ''
    UNION ALL
    SELECT empresa_id, TRIM(categoria_a)
    FROM regla_descuento_combo
    WHERE categoria_a IS NOT NULL AND TRIM(categoria_a) <> ''
    UNION ALL
    SELECT empresa_id, TRIM(categoria_b)
    FROM regla_descuento_combo
    WHERE categoria_b IS NOT NULL AND TRIM(categoria_b) <> ''
    UNION ALL
    SELECT empresa_id, TRIM(nombre)
    FROM tienda_categoria
    WHERE nombre IS NOT NULL AND TRIM(nombre) <> ''
) todas_las_categorias
GROUP BY empresa_id, LOWER(nombre_trim);

-- Semilla de subcategoria: mismo criterio, pero agrupada además por su
-- categoría padre (LOWER(categoria) + LOWER(subcategoria)), tomando la unión
-- de pares (categoria, subcategoria) de producto y de los dos lados de
-- regla_descuento_combo (tienda_categoria no tiene subcategoría).
INSERT INTO subcategoria (categoria_id, nombre)
SELECT c.id, pares.nombre
FROM (
    SELECT empresa_id, LOWER(TRIM(categoria_txt)) AS categoria_key, MIN(TRIM(subcategoria_txt)) AS nombre
    FROM (
        SELECT empresa_id, categoria AS categoria_txt, subcategoria AS subcategoria_txt FROM producto
        UNION ALL
        SELECT empresa_id, categoria_a, subcategoria_a FROM regla_descuento_combo
        UNION ALL
        SELECT empresa_id, categoria_b, subcategoria_b FROM regla_descuento_combo
    ) todos_los_pares
    WHERE categoria_txt IS NOT NULL AND TRIM(categoria_txt) <> ''
      AND subcategoria_txt IS NOT NULL AND TRIM(subcategoria_txt) <> ''
    GROUP BY empresa_id, LOWER(TRIM(categoria_txt)), LOWER(TRIM(subcategoria_txt))
) pares
JOIN categoria c ON c.empresa_id = pares.empresa_id AND LOWER(c.nombre) = pares.categoria_key;

-- === producto: categoria/subcategoria de texto -> categoria_id/subcategoria_id ===

ALTER TABLE producto ADD COLUMN categoria_id BIGINT NULL;
ALTER TABLE producto ADD COLUMN subcategoria_id BIGINT NULL;

UPDATE producto p
JOIN categoria c ON c.empresa_id = p.empresa_id AND LOWER(c.nombre) = LOWER(TRIM(p.categoria))
SET p.categoria_id = c.id;

UPDATE producto p
JOIN subcategoria s ON s.categoria_id = p.categoria_id AND LOWER(s.nombre) = LOWER(TRIM(p.subcategoria))
SET p.subcategoria_id = s.id
WHERE p.subcategoria IS NOT NULL AND TRIM(p.subcategoria) <> '';

-- categoria_id NOT NULL: todo producto tiene categoría (columna vieja ya era
-- NOT NULL). subcategoria_id queda nullable: no todo producto tiene
-- subcategoría cargada.
ALTER TABLE producto MODIFY COLUMN categoria_id BIGINT NOT NULL;
ALTER TABLE producto ADD CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id);
ALTER TABLE producto ADD CONSTRAINT fk_producto_subcategoria FOREIGN KEY (subcategoria_id) REFERENCES subcategoria (id);

CREATE INDEX idx_producto_categoria ON producto (categoria_id);

ALTER TABLE producto DROP COLUMN categoria;
ALTER TABLE producto DROP COLUMN subcategoria;

-- === regla_descuento_combo: mismo criterio, 4 columnas (2 lados x categoria/subcategoria) ===

ALTER TABLE regla_descuento_combo ADD COLUMN categoria_a_id BIGINT NULL;
ALTER TABLE regla_descuento_combo ADD COLUMN subcategoria_a_id BIGINT NULL;
ALTER TABLE regla_descuento_combo ADD COLUMN categoria_b_id BIGINT NULL;
ALTER TABLE regla_descuento_combo ADD COLUMN subcategoria_b_id BIGINT NULL;

UPDATE regla_descuento_combo r
JOIN categoria ca ON ca.empresa_id = r.empresa_id AND LOWER(ca.nombre) = LOWER(TRIM(r.categoria_a))
SET r.categoria_a_id = ca.id;

UPDATE regla_descuento_combo r
JOIN subcategoria sa ON sa.categoria_id = r.categoria_a_id AND LOWER(sa.nombre) = LOWER(TRIM(r.subcategoria_a))
SET r.subcategoria_a_id = sa.id
WHERE r.subcategoria_a IS NOT NULL AND TRIM(r.subcategoria_a) <> '';

UPDATE regla_descuento_combo r
JOIN categoria cb ON cb.empresa_id = r.empresa_id AND LOWER(cb.nombre) = LOWER(TRIM(r.categoria_b))
SET r.categoria_b_id = cb.id;

UPDATE regla_descuento_combo r
JOIN subcategoria sb ON sb.categoria_id = r.categoria_b_id AND LOWER(sb.nombre) = LOWER(TRIM(r.subcategoria_b))
SET r.subcategoria_b_id = sb.id
WHERE r.subcategoria_b IS NOT NULL AND TRIM(r.subcategoria_b) <> '';

ALTER TABLE regla_descuento_combo MODIFY COLUMN categoria_a_id BIGINT NOT NULL;
ALTER TABLE regla_descuento_combo MODIFY COLUMN categoria_b_id BIGINT NOT NULL;
ALTER TABLE regla_descuento_combo ADD CONSTRAINT fk_regladescuentocombo_categoria_a FOREIGN KEY (categoria_a_id) REFERENCES categoria (id);
ALTER TABLE regla_descuento_combo ADD CONSTRAINT fk_regladescuentocombo_subcategoria_a FOREIGN KEY (subcategoria_a_id) REFERENCES subcategoria (id);
ALTER TABLE regla_descuento_combo ADD CONSTRAINT fk_regladescuentocombo_categoria_b FOREIGN KEY (categoria_b_id) REFERENCES categoria (id);
ALTER TABLE regla_descuento_combo ADD CONSTRAINT fk_regladescuentocombo_subcategoria_b FOREIGN KEY (subcategoria_b_id) REFERENCES subcategoria (id);

ALTER TABLE regla_descuento_combo DROP COLUMN categoria_a;
ALTER TABLE regla_descuento_combo DROP COLUMN subcategoria_a;
ALTER TABLE regla_descuento_combo DROP COLUMN categoria_b;
ALTER TABLE regla_descuento_combo DROP COLUMN subcategoria_b;

-- === tienda_categoria: nombre de texto -> categoria_id ===

ALTER TABLE tienda_categoria ADD COLUMN categoria_id BIGINT NULL;

UPDATE tienda_categoria tc
JOIN categoria c ON c.empresa_id = tc.empresa_id AND LOWER(c.nombre) = LOWER(TRIM(tc.nombre))
SET tc.categoria_id = c.id;

ALTER TABLE tienda_categoria MODIFY COLUMN categoria_id BIGINT NOT NULL;
ALTER TABLE tienda_categoria ADD CONSTRAINT fk_tiendacategoria_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id);

-- Reemplaza el unique viejo (empresa_id, nombre) por el nuevo (empresa_id,
-- categoria_id): la categoría real ahora es la FK, no el texto.
ALTER TABLE tienda_categoria DROP INDEX uk_tiendacategoria_empresa_nombre;
ALTER TABLE tienda_categoria ADD CONSTRAINT uk_tiendacategoria_empresa_categoria UNIQUE (empresa_id, categoria_id);

ALTER TABLE tienda_categoria DROP COLUMN nombre;
