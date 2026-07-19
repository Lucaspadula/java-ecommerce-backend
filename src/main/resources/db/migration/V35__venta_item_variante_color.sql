-- Snapshot del color vendido (Etapa 2 de variantes): venta_item.variante_id
-- (V34) ya identifica QUÉ fila de producto_variante se vendió, pero esa fila
-- se puede borrar/renombrar después (el dueño discontinúa un color). Este
-- campo guarda el nombre tal cual estaba al momento de la venta, para que
-- VentaServiceImpl.construirTextoCompartir (el mensaje de WhatsApp) siga
-- mostrando el color correcto en pedidos viejos sin depender de un join en
-- vivo — mismo criterio que venta_item.producto_nombre.
ALTER TABLE venta_item ADD COLUMN variante_color VARCHAR(60) NULL;
