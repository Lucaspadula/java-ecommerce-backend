-- El dueño recibe reseñas reales por WhatsApp y quiere poder adjuntar la
-- captura de pantalla como respaldo visual opcional de la reseña, además
-- del comentario de texto (que sigue siendo obligatorio). Nullable: las
-- reseñas ya cargadas sin foto quedan intactas.

ALTER TABLE resena ADD COLUMN imagen_url VARCHAR(500) NULL;
