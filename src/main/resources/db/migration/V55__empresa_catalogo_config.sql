ALTER TABLE empresa
    ADD COLUMN catalogo_mostrar_logo BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN catalogo_titulo_personalizado VARCHAR(255) NULL,
    ADD COLUMN catalogo_mostrar_descripcion BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN catalogo_mostrar_colores BOOLEAN NOT NULL DEFAULT TRUE;
