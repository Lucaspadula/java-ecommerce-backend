-- Reseñas dejadas por el CLIENTE (compra verificada), no solo por el dueño:
-- puntuacion (1-5, default 5 para no romper las reseñas viejas cargadas a
-- mano, que no tenían rating) y venta_id (nullable: esas reseñas viejas del
-- admin siguen sin venta asociada; las nuevas del cliente sí la tienen, y
-- sirve para no dejar 2 reseñas de la misma compra). comentario pasa a ser
-- opcional: el cliente puede dejar solo las estrellas, sin texto.
ALTER TABLE resena
    ADD COLUMN puntuacion TINYINT NOT NULL DEFAULT 5,
    ADD COLUMN venta_id BIGINT NULL,
    MODIFY COLUMN comentario VARCHAR(1000) NULL,
    ADD CONSTRAINT fk_resena_venta FOREIGN KEY (venta_id) REFERENCES venta (id);

CREATE INDEX idx_resena_venta_id ON resena (venta_id);
