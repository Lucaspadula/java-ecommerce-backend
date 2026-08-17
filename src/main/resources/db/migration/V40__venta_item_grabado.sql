-- Imagen adjunta al pedido de grabado personalizado (logo/diseño que el
-- cliente sube en el checkout público). El texto del grabado reusa la
-- columna personalizacion que ya existía (pensada para el admin, nunca
-- conectada al checkout público hasta ahora) — solo la imagen necesita
-- columna nueva, porque personalizacion no existía para eso.
ALTER TABLE venta_item
    ADD COLUMN grabado_imagen_url VARCHAR(255) NULL;
