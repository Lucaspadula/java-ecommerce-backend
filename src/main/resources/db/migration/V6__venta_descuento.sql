-- Agrega descuento porcentual a Venta. El total pasa a ser el monto DESPUÉS
-- de aplicar el descuento sobre el subtotal (suma de items); el subtotal se
-- persiste aparte para no tener que recalcularlo desde los items cada vez
-- que se arma el DTO.

ALTER TABLE venta ADD COLUMN subtotal DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE venta ADD COLUMN descuento_porcentaje DECIMAL(5,2) NOT NULL DEFAULT 0;
