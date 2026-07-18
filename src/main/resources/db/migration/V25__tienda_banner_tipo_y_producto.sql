-- Los banners de la tienda pública dejan de ser solo el hero rotativo: ahora
-- también puede haber banners verticales (sección aparte, ver
-- Empresa.tiendaBannerVerticalPosicion más abajo), y cualquiera de los dos
-- tipos puede llevar asociado un producto opcional (click en el banner abre
-- el modal de detalle de ESE producto). `tipo` discrimina entre ambos en la
-- misma tabla en vez de duplicarla: es el mismo modelo de datos con un
-- distinto significado de `orden` y un límite de cantidad distinto por tipo
-- (ver PerfilServiceImpl.MAX_BANNER_IMAGENES / MAX_BANNER_VERTICAL_IMAGENES).
-- DEFAULT 'HERO' es clave: así las filas ya cargadas del banner rotativo
-- existente quedan migradas automáticamente, sin script de datos aparte.
ALTER TABLE tienda_banner_imagen ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'HERO';
ALTER TABLE tienda_banner_imagen ADD COLUMN producto_id BIGINT NULL;
ALTER TABLE tienda_banner_imagen ADD CONSTRAINT fk_tiendabannerimagen_producto FOREIGN KEY (producto_id) REFERENCES producto (id);

-- Ubicación elegida por el admin para la sección de banners verticales
-- dentro de la tienda pública (ver ActualizarTiendaRequest/PublicEmpresaDto).
-- Nullable a propósito: mientras no se elija una ubicación, el frontend
-- público no debe renderizar la sección aunque ya haya imágenes verticales
-- cargadas (evita que aparezca en un lugar no elegido la primera vez que se
-- sube algo). Valores esperados: DESPUES_BANNER, DESPUES_CATEGORIAS,
-- DESPUES_DESTACADOS, ANTES_FOOTER (validados solo en el frontend, mismo
-- criterio que tienda_fuente).
ALTER TABLE empresa ADD COLUMN tienda_banner_vertical_posicion VARCHAR(30) NULL;
