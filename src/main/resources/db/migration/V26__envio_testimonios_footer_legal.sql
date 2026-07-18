-- Paquete de mejoras chicas sobre la tienda pública:
-- 1) Datos de envío opcionales en el pedido (solo informativos para
--    coordinar por WhatsApp, no hay cálculo de costo de envío).
-- 5) Testimonios fijos del negocio (no ligados a un producto, a diferencia
--    de "resena" que sí lo está).
-- 7/8) Más presencia legal + texto "Sobre nosotros" en el footer/tienda.

ALTER TABLE venta
    ADD COLUMN direccion_envio VARCHAR(200) NULL,
    ADD COLUMN localidad       VARCHAR(150) NULL,
    ADD COLUMN codigo_postal   VARCHAR(20)  NULL;

ALTER TABLE empresa
    ADD COLUMN tienda_razon_social    VARCHAR(150) NULL,
    ADD COLUMN tienda_cuit            VARCHAR(20)  NULL,
    ADD COLUMN tienda_direccion       VARCHAR(200) NULL,
    ADD COLUMN tienda_sobre_nosotros  TEXT         NULL;

-- Testimonios generales del negocio, cargados a mano por el dueño (nombre +
-- comentario, sin imagen) — separados de "resena" porque esos SÍ están
-- ligados a un producto puntual. `orden` define el orden de aparición en la
-- franja de la tienda pública (agregar al final = mayor valor, ver
-- TiendaTestimonioServiceImpl).
CREATE TABLE tienda_testimonio (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    empresa_id      BIGINT       NOT NULL,
    cliente_nombre  VARCHAR(150) NOT NULL,
    comentario      VARCHAR(500) NOT NULL,
    orden           INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_tiendatestimonio_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

CREATE INDEX idx_tiendatestimonio_empresa ON tienda_testimonio (empresa_id);
