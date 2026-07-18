-- Reglas de descuento automático por combinación de categorías (ej. "Mates +
-- Bombillas = 5%"), configuradas a mano por el admin. Categoría/subcategoría
-- son texto libre (igual que producto.categoria/subcategoria, no hay tabla
-- maestra), por eso se relacionan por nombre, no por FK. La subcategoría de
-- cada lado es opcional: si viene NULL, la regla matchea cualquier producto
-- de esa categoría sin importar la subcategoría (ver
-- CalculadorDescuentoComboService).
CREATE TABLE regla_descuento_combo (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    empresa_id      BIGINT        NOT NULL,
    categoria_a     VARCHAR(100)  NOT NULL,
    subcategoria_a  VARCHAR(100)  NULL,
    categoria_b     VARCHAR(100)  NOT NULL,
    subcategoria_b  VARCHAR(100)  NULL,
    porcentaje      DECIMAL(5,2)  NOT NULL,
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_regladescuentocombo_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

CREATE INDEX idx_regladescuentocombo_empresa ON regla_descuento_combo (empresa_id);

-- Monto (no otro %) porque el descuento combo aplica solo sobre el precio de
-- los productos que forman cada par específico, no sobre el total del
-- carrito como sí hace tienda_cupon_porcentaje (V17). `detalle` guarda un
-- snapshot legible ("Mates + Bombillas") para mostrarlo en el pedido
-- confirmado sin tener que re-resolver la regla (que puede haberse borrado o
-- editado después).
ALTER TABLE venta
    ADD COLUMN descuento_combo_monto   DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN descuento_combo_detalle VARCHAR(255)  NULL;
