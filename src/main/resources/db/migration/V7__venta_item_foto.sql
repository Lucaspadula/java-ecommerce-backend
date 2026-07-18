-- Foto de referencia opcional para la personalización/grabado de un item de
-- Venta. Se sube vía endpoint genérico (POST /api/ventas/fotos) antes de que
-- el item exista como fila persistida — ver VentaServiceImpl.subirFoto().

ALTER TABLE venta_item ADD COLUMN foto_url VARCHAR(255) NULL;
