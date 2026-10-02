-- Retiro del esquema viejo (PR4 de bloques-tienda). V63 ya copió los tips y los
-- banners VERTICAL a tienda_bloque / tienda_bloque_card (con origen_legacy), y
-- la tienda pública ya lee solo bloques. Acá se borra lo que quedó sin uso:
--   * tienda_tip (tabla completa),
--   * las filas VERTICAL de tienda_banner_imagen (el HERO NO se toca),
--   * empresa.tienda_banner_vertical_posicion.
-- NO toca producto, categoria, subcategoria, venta, cliente ni el HERO. Los
-- archivos subidos (uploads/) tampoco se borran: las cards migradas los siguen
-- usando. Reversión manual: db/rollback/R64__restaurar_tips_y_verticales.sql
-- (fuera de Flyway), que reconstruye desde las cards TIP:% y BANNER:%.

DROP TABLE tienda_tip;

DELETE FROM tienda_banner_imagen WHERE tipo = 'VERTICAL';

ALTER TABLE empresa DROP COLUMN tienda_banner_vertical_posicion;
