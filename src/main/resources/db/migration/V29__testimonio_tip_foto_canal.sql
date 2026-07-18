-- El dueño puede opcionalmente adjuntar una foto (mismo patrón que
-- Resena.imagen_url) a un testimonio y elegir por qué canal llegó ese
-- comentario (WhatsApp/Instagram/Otro), para mostrar el ícono
-- correspondiente en la tienda pública. Ambos nullable: los testimonios ya
-- cargados quedan intactos, sin foto ni canal.
ALTER TABLE tienda_testimonio
    ADD COLUMN foto_url VARCHAR(255) NULL,
    ADD COLUMN canal VARCHAR(20) NULL;

-- Mismo criterio de foto opcional para un tip: si no se carga, la tienda
-- pública muestra un rectángulo neutro (--surface/--line) en su lugar (ver
-- tienda-publica.css .tip-card), no rompe nada de lo ya cargado.
ALTER TABLE tienda_tip
    ADD COLUMN foto_url VARCHAR(255) NULL;
