-- Reemplaza el campo único empresa.tienda_banner_imagen_url por una galería
-- de imágenes rotables (carrusel con Ken Burns + crossfade en la tienda
-- pública, ver PublicTiendaServiceImpl/tienda-publica.ts). Cada empresa
-- puede tener hasta 6 imágenes (ver PerfilServiceImpl.agregarBannerImagen),
-- ordenadas por el campo `orden`.

CREATE TABLE tienda_banner_imagen (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id  BIGINT       NOT NULL,
    imagen_url  VARCHAR(500) NOT NULL,
    orden       INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_tiendabannerimagen_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

CREATE INDEX idx_tiendabannerimagen_empresa ON tienda_banner_imagen (empresa_id);

-- Migra la imagen de banner que ya tenía cada empresa (si la tenía) como la
-- primera (orden 0) de su nueva galería, para no perderla. Mismo criterio de
-- migración de datos real que V21 (INSERT ... SELECT puro).
INSERT INTO tienda_banner_imagen (empresa_id, imagen_url, orden)
SELECT e.id, e.tienda_banner_imagen_url, 0
FROM empresa e
WHERE e.tienda_banner_imagen_url IS NOT NULL;

-- Con los datos ya migrados a la tabla nueva, el campo queda completamente
-- reemplazado: la galería es la única fuente del banner de ahora en más.
ALTER TABLE empresa DROP COLUMN tienda_banner_imagen_url;
