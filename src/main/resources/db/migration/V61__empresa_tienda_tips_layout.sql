-- Disposición de la sección "Cuidá tu mate" de la tienda pública, análogo a
-- tienda_tema (ver Empresa.tiendaTipsLayout). Valores válidos: vertical-1,
-- vertical-2, horizontal.
ALTER TABLE empresa
    ADD COLUMN tienda_tips_layout VARCHAR(20) NOT NULL DEFAULT 'vertical-1';
