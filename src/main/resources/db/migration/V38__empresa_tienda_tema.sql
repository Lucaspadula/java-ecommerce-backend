-- Paleta de color de marca para la tienda pública, análogo a tienda_fuente
-- (ver Empresa.tiendaTema). Valores válidos: claro, oscuro, negro-dorado,
-- marino-dorado, ciruela-oliva.
ALTER TABLE empresa
    ADD COLUMN tienda_tema VARCHAR(20) NOT NULL DEFAULT 'claro';
