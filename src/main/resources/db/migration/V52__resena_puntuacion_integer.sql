-- V50 creó `puntuacion` como TINYINT, pero Resena.puntuacion es un Integer
-- (mapea a INTEGER) — Hibernate en modo validate (no update) rechaza el
-- mismatch al arrancar. TINYINT alcanza de sobra para 1-5, pero corregir acá
-- en vez de editar V50 (ya aplicada) es el único camino seguro con Flyway.
ALTER TABLE resena
    MODIFY COLUMN puntuacion INT NOT NULL DEFAULT 5;
