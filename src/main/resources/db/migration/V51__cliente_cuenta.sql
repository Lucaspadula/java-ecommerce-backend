-- Cuenta de cliente: reusa la misma tabla `cliente` que ya usan las ventas
-- (identificada hoy por teléfono) en vez de una tabla nueva, así que
-- registrarse con el mismo teléfono de una compra anterior deja esa cuenta
-- ya vinculada a su historial, sin paso de "linkeo" aparte.
-- Ambos nullable: un Cliente creado por una venta de invitado (crearPedido)
-- no tiene ninguno de los dos hasta que decide crear una cuenta.
ALTER TABLE cliente
    ADD COLUMN password_hash VARCHAR(255) NULL,
    ADD COLUMN google_sub VARCHAR(255) NULL;

CREATE UNIQUE INDEX idx_cliente_google_sub ON cliente (google_sub);
