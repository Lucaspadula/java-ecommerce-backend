-- Tips de cuidado del mate: contenido curado a mano por el dueño (título +
-- texto), mostrado en una franja fija de la home de la tienda pública,
-- inmediatamente después de la franja de testimonios. Mismo criterio que
-- tienda_testimonio (V26): sin imagen, `orden` define el orden de aparición
-- (agregar al final = mayor valor, ver TiendaTipServiceImpl). A diferencia de
-- testimonio, no hay autor asociado (no es una cita de un cliente), por eso
-- no tiene un campo equivalente a cliente_nombre.
CREATE TABLE tienda_tip (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    empresa_id  BIGINT       NOT NULL,
    titulo      VARCHAR(150) NOT NULL,
    contenido   VARCHAR(500) NOT NULL,
    orden       INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_tiendatip_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

CREATE INDEX idx_tiendatip_empresa ON tienda_tip (empresa_id);
