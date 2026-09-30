-- Toggle manual por foto: agrandada=false (default) muestra la foto COMPLETA
-- (object-fit: contain en el front), agrandada=true la agranda para llenar
-- el marco aunque recorte bordes (object-fit: cover). Antes esto lo decidía
-- el CSS solo; ahora lo elige el dueño foto por foto (ver
-- ProductoServiceImpl.ajustarFoto).
ALTER TABLE producto_foto ADD COLUMN agrandada BOOLEAN NOT NULL DEFAULT FALSE;
