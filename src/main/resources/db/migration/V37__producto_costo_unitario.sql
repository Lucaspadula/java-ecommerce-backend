-- Costo propio, opcional: solo lo usan los productos SIN receta de insumos
-- (ver Producto.costoUnitario / ProductoServiceImpl.costoEfectivo). Un
-- producto con receta sigue calculando su costo sumando insumos, como
-- siempre — esta columna queda en null para esos casos.
ALTER TABLE producto
    ADD COLUMN costo_unitario DECIMAL(12, 2) NULL;
