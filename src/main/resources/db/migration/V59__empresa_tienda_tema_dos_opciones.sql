-- Se redujo la paleta de la tienda pública a 2 temas (claro/oscuro) — ver
-- Empresa.tiendaTema. Las empresas que tenían elegido negro-dorado,
-- marino-dorado, ciruela-oliva o salvia-terracota (ya sin CSS que los pinte)
-- caen al oscuro, la variante más cercana visualmente a esos temas oscuros.
UPDATE empresa
    SET tienda_tema = 'oscuro'
    WHERE tienda_tema NOT IN ('claro', 'oscuro');
