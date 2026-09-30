-- Estilo por bloque de las páginas "Importante"/"Cómo comprar" del catálogo
-- en PDF: hasta ahora el único estilo era GLOBAL (Empresa.catalogo_texto_*,
-- V57), un único font/tamaño/color para TODOS los puntos de ambas páginas.
-- Esto agrega alineación + negrita/cursiva/subrayado POR PUNTO, para poder
-- destacar un punto puntual distinto del resto (ver TiendaCatalogoSeccion).
ALTER TABLE tienda_catalogo_seccion
    ADD COLUMN alineacion VARCHAR(10) NOT NULL DEFAULT 'IZQUIERDA',
    ADD COLUMN negrita BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN cursiva BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN subrayado BOOLEAN NOT NULL DEFAULT FALSE;
