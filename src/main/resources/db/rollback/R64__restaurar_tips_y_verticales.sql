-- REVERSIÓN MANUAL de V64 (NO es una migración de Flyway: vive fuera de
-- db/migration y no se ejecuta sola). Reconstruye el esquema viejo y sus datos
-- desde las cards migradas por V63 (origen_legacy 'TIP:{id}' y 'BANNER:{id}').
-- Orden de uso: 1) restaurar el backup previo a V64 si hay que recuperar TODO
-- tal cual estaba, o 2) correr este script sobre el esquema posterior a V64 para
-- reconstruir desde los bloques. Limitaciones: solo se restauran las cards que
-- vienen de la migración (las cards creadas luego desde el panel no tienen
-- origen_legacy y se ignoran) y los cambios hechos a esas cards después de V63
-- se reflejan tal como están hoy. Después de correrlo, para volver a tener
-- Flyway consistente hay que borrar la fila de V64 de flyway_schema_history.
-- Se ejecuta una sola vez (usa CREATE/ADD COLUMN planos, no IF NOT EXISTS).

-- 1) Tabla de tips (V28 + foto_url de V29).
CREATE TABLE tienda_tip (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    empresa_id  BIGINT       NOT NULL,
    titulo      VARCHAR(150) NOT NULL,
    contenido   VARCHAR(500) NOT NULL,
    orden       INT          NOT NULL DEFAULT 0,
    foto_url    VARCHAR(255) NULL,
    CONSTRAINT fk_tiendatip_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

CREATE INDEX idx_tiendatip_empresa ON tienda_tip (empresa_id);

-- 2) Columna de ubicación de los banners verticales (V25).
ALTER TABLE empresa ADD COLUMN tienda_banner_vertical_posicion VARCHAR(30) NULL;

-- 3) Tips: cada card TIP:{id} vuelve a su fila original (mismo id).
INSERT INTO tienda_tip (id, empresa_id, titulo, contenido, orden, foto_url)
SELECT CAST(SUBSTRING(c.origen_legacy, 5) AS UNSIGNED),
       bl.empresa_id,
       COALESCE(c.titulo, ''),
       COALESCE(c.texto, ''),
       c.orden,
       c.imagen_url
FROM tienda_bloque_card c
JOIN tienda_bloque bl ON bl.id = c.bloque_id
WHERE c.origen_legacy LIKE 'TIP:%'
  AND NOT EXISTS (SELECT 1 FROM tienda_tip t WHERE t.id = CAST(SUBSTRING(c.origen_legacy, 5) AS UNSIGNED));

-- 4) Banners verticales: cada card BANNER:{id} vuelve como fila VERTICAL (mismo
-- id). producto_id solo si la acción era PRODUCTO y el producto todavía existe.
INSERT INTO tienda_banner_imagen (id, empresa_id, imagen_url, orden, tipo, producto_id)
SELECT CAST(SUBSTRING(c.origen_legacy, 8) AS UNSIGNED),
       bl.empresa_id,
       c.imagen_url,
       c.orden,
       'VERTICAL',
       p.id
FROM tienda_bloque_card c
JOIN tienda_bloque bl ON bl.id = c.bloque_id
LEFT JOIN producto p ON c.accion = 'PRODUCTO' AND p.id = CAST(c.accion_valor AS UNSIGNED) AND p.empresa_id = bl.empresa_id
WHERE c.origen_legacy LIKE 'BANNER:%'
  AND c.imagen_url IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tienda_banner_imagen b WHERE b.id = CAST(SUBSTRING(c.origen_legacy, 8) AS UNSIGNED));

-- 5) Posición: slot del bloque BANNER_VERTICAL (HOME_{posicion}) si está activo;
-- inactivo = sin posición (null), como era antes de migrar.
UPDATE empresa e
JOIN tienda_bloque b ON b.empresa_id = e.id AND b.origen_legacy = 'BANNER_VERTICAL'
SET e.tienda_banner_vertical_posicion = CASE WHEN b.activo THEN SUBSTRING(b.slot, 6) ELSE NULL END;
