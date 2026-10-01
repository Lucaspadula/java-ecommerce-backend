-- Migración de DATOS (V62 creó las tablas): banners verticales y tips pasan a
-- bloques configurables. Es aditiva: NO borra ni modifica tienda_tip, los
-- banners VERTICAL, el HERO, empresa.tienda_banner_vertical_posicion ni nada
-- del catálogo. El retiro del esquema viejo viene en una migración posterior.
-- Idempotente: cada INSERT ... SELECT lleva NOT EXISTS sobre origen_legacy
-- (bloques BANNER_VERTICAL / TIPS; cards BANNER:{id} / TIP:{id}), más los
-- UNIQUE de V62 como cinturón. Re-ejecutarla no cambia los conteos.

-- 1) Un bloque "Elegidos para vos" (título hoy fijo en la tienda) por empresa con
-- banners VERTICAL. Posición reconocida => HOME_{posicion} y activo; posición
-- null o no reconocida => HOME_DESPUES_DESTACADOS e inactivo, porque hoy con
-- posición null la sección no se muestra (se preserva el aspecto).
INSERT INTO tienda_bloque (empresa_id, titulo, slot, ancho, orden, activo, origen_legacy)
SELECT e.id,
       'Elegidos para vos',
       CASE WHEN e.tienda_banner_vertical_posicion IN ('DESPUES_BANNER', 'DESPUES_CATEGORIAS', 'DESPUES_DESTACADOS', 'ANTES_FOOTER')
            THEN CONCAT('HOME_', e.tienda_banner_vertical_posicion)
            ELSE 'HOME_DESPUES_DESTACADOS' END,
       'COMPLETO',
       0,
       CASE WHEN e.tienda_banner_vertical_posicion IN ('DESPUES_BANNER', 'DESPUES_CATEGORIAS', 'DESPUES_DESTACADOS', 'ANTES_FOOTER')
            THEN TRUE ELSE FALSE END,
       'BANNER_VERTICAL'
FROM empresa e
WHERE EXISTS (SELECT 1 FROM tienda_banner_imagen b WHERE b.empresa_id = e.id AND b.tipo = 'VERTICAL')
  AND NOT EXISTS (SELECT 1 FROM tienda_bloque x WHERE x.empresa_id = e.id AND x.origen_legacy = 'BANNER_VERTICAL');

-- 2) Cada banner VERTICAL pasa a una card VERTICAL con su imagen y orden;
-- con producto_id la acción es PRODUCTO (valor = id), si no NINGUNA.
INSERT INTO tienda_bloque_card (bloque_id, imagen_url, orientacion, orden, accion, accion_valor, origen_legacy)
SELECT bl.id,
       b.imagen_url,
       'VERTICAL',
       b.orden,
       CASE WHEN b.producto_id IS NULL THEN 'NINGUNA' ELSE 'PRODUCTO' END,
       CAST(b.producto_id AS CHAR),
       CONCAT('BANNER:', b.id)
FROM tienda_banner_imagen b
JOIN tienda_bloque bl ON bl.empresa_id = b.empresa_id AND bl.origen_legacy = 'BANNER_VERTICAL'
WHERE b.tipo = 'VERTICAL'
  AND NOT EXISTS (SELECT 1 FROM tienda_bloque_card c WHERE c.bloque_id = bl.id AND c.origen_legacy = CONCAT('BANNER:', b.id));

-- 3) Un bloque "Cuidá tu mate" por empresa con tips, al final de la home
-- (orden 1: posterior al bloque de banners migrado, que tiene orden 0).
INSERT INTO tienda_bloque (empresa_id, titulo, slot, ancho, orden, activo, origen_legacy)
SELECT e.id, 'Cuidá tu mate', 'HOME_ANTES_FOOTER', 'COMPLETO', 1, TRUE, 'TIPS'
FROM empresa e
WHERE EXISTS (SELECT 1 FROM tienda_tip t WHERE t.empresa_id = e.id)
  AND NOT EXISTS (SELECT 1 FROM tienda_bloque x WHERE x.empresa_id = e.id AND x.origen_legacy = 'TIPS');

-- 4) Cada tip pasa a una card con acción MODAL (título, contenido -> texto, foto, orden).
INSERT INTO tienda_bloque_card (bloque_id, imagen_url, orientacion, titulo, texto, orden, accion, accion_valor, origen_legacy)
SELECT bl.id, t.foto_url, 'VERTICAL', t.titulo, t.contenido, t.orden, 'MODAL', NULL, CONCAT('TIP:', t.id)
FROM tienda_tip t
JOIN tienda_bloque bl ON bl.empresa_id = t.empresa_id AND bl.origen_legacy = 'TIPS'
WHERE NOT EXISTS (SELECT 1 FROM tienda_bloque_card c WHERE c.bloque_id = bl.id AND c.origen_legacy = CONCAT('TIP:', t.id));
