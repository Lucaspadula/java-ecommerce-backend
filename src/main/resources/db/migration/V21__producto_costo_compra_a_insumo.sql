-- Unifica el modelo de costo/stock: de ahora en más TODO producto se
-- compone de insumos, sin excepción. Los productos "comprados ya hechos"
-- (costo_compra cargado a mano, sin receta) migran a un insumo nuevo que
-- representa el producto entero, con una receta de una sola línea
-- (cantidad 1). Recién después de migrar los datos se elimina la columna
-- producto.costo_compra: el costo de TODO producto pasa a calcularse
-- siempre como suma de insumos (ver ProductoServiceImpl.calcularCostoUnitario).
--
-- producto.stock NO se toca acá: ya quedó como columna de esquema sin usar
-- desde que el stock se deriva de insumos (ver StockDisponibleCalculator),
-- mismo criterio de la migración anterior.

-- 1) Un insumo nuevo por cada producto con costo_compra cargado. El nombre
--    generado incluye el id del producto —
--    CONCAT(LEFT(p.nombre, 120), ' (comprado #', p.id, ')') — a propósito:
--    garantiza una clave de join única para el paso 2 aunque dos productos
--    de la misma empresa tengan el mismo nombre. Un simple
--    CONCAT(nombre, ' (comprado)') sin el id se rompería en ese caso (el
--    UPDATE/JOIN de abajo podría emparejar un producto con el insumo de
--    OTRO producto homónimo, mezclando costo/stock entre productos
--    distintos). El costo_unitario y el stock salen tal cual estaban
--    cargados en el producto (p.costo_compra y p.stock respectivamente) —
--    mismo valor, ahora vive en el lugar correcto.
INSERT INTO insumo (empresa_id, nombre, costo_unitario, stock, stock_minimo, unidad_medida, activo, fecha_alta)
SELECT p.empresa_id,
       CONCAT(LEFT(p.nombre, 120), ' (comprado #', p.id, ')'),
       p.costo_compra,
       p.stock,
       NULL,
       'unidad',
       TRUE,
       CURRENT_TIMESTAMP
FROM producto p
WHERE p.costo_compra IS NOT NULL;

-- 2) Receta de una línea (cantidad 1) que vincula cada producto migrado con
--    el insumo recién creado en el paso 1. El join por (empresa_id, nombre)
--    es seguro acá porque el nombre generado arriba es único por
--    construcción (incluye el id del producto), así que cada producto solo
--    puede matchear con el insumo que se creó específicamente para él.
INSERT INTO producto_insumo (producto_id, insumo_id, cantidad)
SELECT p.id, i.id, 1
FROM producto p
JOIN insumo i
  ON i.empresa_id = p.empresa_id
 AND i.nombre = CONCAT(LEFT(p.nombre, 120), ' (comprado #', p.id, ')')
WHERE p.costo_compra IS NOT NULL;

-- 3) Con los datos ya migrados a insumo/producto_insumo, costo_compra deja
--    de tener uso: el costo de todo producto sale siempre de sumar sus
--    insumos.
ALTER TABLE producto DROP COLUMN costo_compra;
