-- Composite (GoF): permite que un Producto (el "kit") se componga de otros
-- Productos ("componentes"), cada uno con una cantidad. Ej: "Combo" = 1x
-- Mate + 1x Termo. Antes de esto, un combo se armaba con un Insumo como
-- sustituto de un Producto terminado (un hack) porque no existía forma de
-- que un Producto referenciara a OTRO Producto.
--
-- Un producto SIN filas acá (sin componentes) es un producto simple: el
-- comportamiento de siempre, sin ningún cambio (ver VentaServiceImpl y
-- StockDisponibleCalculator, que ramifican explícitamente en ese chequeo
-- antes de tocar cualquier camino existente).
--
-- Esta migración NO migra los combos existentes (armados hoy con el hack de
-- Insumo): esa migración de datos es un paso aparte, pendiente.
CREATE TABLE producto_componente (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id             BIGINT      NOT NULL,
    componente_producto_id  BIGINT      NOT NULL,
    cantidad                INT         NOT NULL DEFAULT 1,
    CONSTRAINT fk_productocomponente_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT fk_productocomponente_componente FOREIGN KEY (componente_producto_id) REFERENCES producto (id)
);

CREATE INDEX idx_productocomponente_producto ON producto_componente (producto_id);
