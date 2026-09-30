-- Pool unificado de fotos por producto (hasta 8, cada una con color opcional
-- vía variante_id): reemplaza el esquema de columnas fijas
-- (producto.foto_url/2/3/4 + producto_variante.foto_url) como fuente de
-- verdad para todo código NUEVO — ver ProductoFotoResolver/ProductoFoto.
--
-- Esta migración es NO DESTRUCTIVA a propósito (ver spec "Migración no
-- destructiva" / design "Migration / Rollout"): crea la tabla y backfillea
-- desde las columnas actuales, pero NO las borra ni las modifica. Así, si
-- hiciera falta revertir el código de la app a la versión anterior a este
-- cambio, esa versión vieja sigue leyendo columnas pobladas y funciona
-- igual que antes, sin necesitar revertir la migración. El DROP de esas
-- columnas queda diferido a una V44 posterior, después de verificar en
-- producción.
CREATE TABLE producto_foto (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id BIGINT       NOT NULL,
    -- Sin FK a propósito, mismo criterio que venta_item.variante_id (V34):
    -- una ProductoVariante SÍ puede borrarse físicamente (orphanRemoval), una
    -- FK acá impediría borrar un color discontinuado que ya tuviera fotos
    -- propias en el pool.
    variante_id BIGINT       NULL,
    url         VARCHAR(255) NOT NULL,
    orden       INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_productofoto_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
);

CREATE INDEX idx_productofoto_producto ON producto_foto (producto_id, orden);

-- Backfill de las 4 columnas fijas de producto (siempre variante_id NULL,
-- "general"), orden 0..3 en el orden fijo que ya tenían como slots. Guardado
-- con NOT EXISTS a propósito: idempotente si este bloque se re-ejecutara
-- (ej. reutilizado a mano en un ambiente de test) no duplica filas.
INSERT INTO producto_foto (producto_id, variante_id, url, orden)
SELECT p.id, NULL, p.foto_url, 0 FROM producto p
WHERE p.foto_url IS NOT NULL AND p.foto_url <> ''
  AND NOT EXISTS (SELECT 1 FROM producto_foto f WHERE f.producto_id = p.id AND f.orden = 0 AND f.variante_id IS NULL);

INSERT INTO producto_foto (producto_id, variante_id, url, orden)
SELECT p.id, NULL, p.foto_url_2, 1 FROM producto p
WHERE p.foto_url_2 IS NOT NULL AND p.foto_url_2 <> ''
  AND NOT EXISTS (SELECT 1 FROM producto_foto f WHERE f.producto_id = p.id AND f.orden = 1 AND f.variante_id IS NULL);

INSERT INTO producto_foto (producto_id, variante_id, url, orden)
SELECT p.id, NULL, p.foto_url_3, 2 FROM producto p
WHERE p.foto_url_3 IS NOT NULL AND p.foto_url_3 <> ''
  AND NOT EXISTS (SELECT 1 FROM producto_foto f WHERE f.producto_id = p.id AND f.orden = 2 AND f.variante_id IS NULL);

INSERT INTO producto_foto (producto_id, variante_id, url, orden)
SELECT p.id, NULL, p.foto_url_4, 3 FROM producto p
WHERE p.foto_url_4 IS NOT NULL AND p.foto_url_4 <> ''
  AND NOT EXISTS (SELECT 1 FROM producto_foto f WHERE f.producto_id = p.id AND f.orden = 3 AND f.variante_id IS NULL);

-- Backfill de la foto propia de cada variante de color: orden 4+ (después de
-- las 4 fijas del producto), consecutivo por producto vía ROW_NUMBER()
-- (requiere MariaDB 10.2+, ya en uso en este proyecto). Guardado por
-- variante_id (único por diseño: cada variante tiene a lo sumo una fila de
-- backfill), mismo criterio idempotente que arriba.
INSERT INTO producto_foto (producto_id, variante_id, url, orden)
SELECT v.producto_id, v.id, v.foto_url, 3 + ROW_NUMBER() OVER (PARTITION BY v.producto_id ORDER BY v.id)
FROM producto_variante v
WHERE v.foto_url IS NOT NULL AND v.foto_url <> ''
  AND NOT EXISTS (SELECT 1 FROM producto_foto f WHERE f.variante_id = v.id);
