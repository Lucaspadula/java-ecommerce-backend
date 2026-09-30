-- Barra de urgencia de la vidriera pública, antes hardcodeada como dato de
-- prueba en el frontend (URGENCIA_DURACION_MS). Ahora configurable por
-- empresa: si tienda_oferta_activa es false o tienda_oferta_fecha_fin es
-- pasada/nula, la vidriera no muestra la barra.
ALTER TABLE empresa
    ADD COLUMN tienda_oferta_activa BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN tienda_oferta_etiqueta VARCHAR(60),
    ADD COLUMN tienda_oferta_texto VARCHAR(150),
    ADD COLUMN tienda_oferta_fecha_fin TIMESTAMP;
