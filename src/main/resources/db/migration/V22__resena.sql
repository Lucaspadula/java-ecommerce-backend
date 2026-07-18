-- Testimonios de clientes que el dueño carga a mano desde el panel de
-- administración para mostrar en la vidriera pública (ej. copia algo que un
-- cliente le dijo por WhatsApp). No son un sistema de reviews público: no
-- hay calificación por estrellas ni endpoint de escritura sin login.

CREATE TABLE resena (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    producto_id     BIGINT       NOT NULL,
    empresa_id      BIGINT       NOT NULL,
    cliente_nombre  VARCHAR(150) NOT NULL,
    comentario      VARCHAR(1000) NOT NULL,
    fecha           DATETIME     NOT NULL,
    CONSTRAINT fk_resena_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT fk_resena_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

CREATE INDEX idx_resena_producto_id ON resena (producto_id);
