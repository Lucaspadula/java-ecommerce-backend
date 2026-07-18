-- Variantes de color de un Producto (Etapa 1): un producto (ej. "Mate
-- Ranchero") puede venir en varios colores (Blanco, Marrón), cada uno con SU
-- PROPIO stock cargado a mano. La receta de insumos del producto (si tiene)
-- sigue siendo compartida entre todas las variantes — ver
-- StockDisponibleCalculator y VentaServiceImpl, que ramifican explícitamente
-- en este chequeo antes de tocar cualquier camino existente.
--
-- Un producto SIN filas acá (sin variantes) es un producto simple: el
-- comportamiento de siempre, sin ningún cambio (stock propio de Producto).
--
-- El color se elige en la tienda pública recién en una etapa aparte (no
-- forma parte de esta migración): esta tabla es solo la base de datos y la
-- carga desde el admin.
CREATE TABLE producto_variante (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id  BIGINT       NOT NULL,
    color        VARCHAR(60)  NOT NULL,
    stock        INT          NOT NULL DEFAULT 0,
    foto_url     VARCHAR(255) NULL,
    CONSTRAINT fk_productovariante_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
);

CREATE INDEX idx_productovariante_producto ON producto_variante (producto_id);

-- venta_item.variante_id (nullable): qué variante puntual se vendió, para
-- descontar SU stock en vez del stock general del producto (ver
-- VentaServiceImpl.demandaPorVariante). Null para productos sin variantes —
-- el comportamiento de hoy no cambia. Sin FK a propósito: a diferencia de
-- Producto (que solo se da de baja lógica, nunca se borra la fila), una
-- ProductoVariante SÍ puede borrarse físicamente si el dueño la saca del
-- form (orphanRemoval, ver ProductoServiceImpl.aplicarVariantes) — una FK
-- acá impediría borrar un color discontinuado si alguna vez se vendió.
ALTER TABLE venta_item ADD COLUMN variante_id BIGINT NULL;
