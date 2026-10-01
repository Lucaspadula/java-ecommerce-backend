-- Bloques configurables de la tienda pública: reemplazan a los banners
-- verticales y a los tips fijos de la home. Un bloque vive en un "slot" (punto
-- de anclaje de una pantalla), tiene un ancho proporcional y contiene cards
-- (imagen + título/texto opcionales + acción al hacer click).
-- Solo DDL: los datos de tips y banners verticales se migran en V63 (el DDL
-- hace commit implícito en MariaDB, así un fallo del DML no deja esto a medias).
-- slot/ancho/orientacion/accion son String validados en TiendaBloqueServiceImpl
-- (convención del proyecto: sin enums de BD).
CREATE TABLE tienda_bloque (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    empresa_id    BIGINT       NOT NULL,
    titulo        VARCHAR(150) NULL,
    slot          VARCHAR(40)  NOT NULL,
    ancho         VARCHAR(20)  NOT NULL DEFAULT 'COMPLETO',
    orden         INT          NOT NULL DEFAULT 0,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Marcador de origen para bloques migrados (BANNER_VERTICAL, TIPS): permite
    -- que V63 sea idempotente y que el retiro posterior sea reversible. NULL en
    -- los bloques nuevos (MariaDB admite varios NULL en un UNIQUE).
    origen_legacy VARCHAR(40)  NULL,
    CONSTRAINT fk_tiendabloque_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT uq_tiendabloque_origen UNIQUE (empresa_id, origen_legacy)
);

CREATE INDEX idx_tiendabloque_empresa_slot_orden ON tienda_bloque (empresa_id, slot, orden);

CREATE TABLE tienda_bloque_card (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    bloque_id     BIGINT       NOT NULL,
    -- Nullable solo por los tips legacy sin foto; el request del admin la exige.
    imagen_url    VARCHAR(500) NULL,
    orientacion   VARCHAR(20)  NOT NULL DEFAULT 'VERTICAL',
    titulo        VARCHAR(150) NULL,
    texto         VARCHAR(500) NULL,
    orden         INT          NOT NULL DEFAULT 0,
    accion        VARCHAR(20)  NOT NULL DEFAULT 'NINGUNA',
    accion_valor  VARCHAR(500) NULL,
    -- BANNER:{id} / TIP:{id} para cards migradas (idempotencia + reversión).
    origen_legacy VARCHAR(40)  NULL,
    CONSTRAINT fk_tiendabloquecard_bloque FOREIGN KEY (bloque_id) REFERENCES tienda_bloque (id) ON DELETE CASCADE,
    CONSTRAINT uq_tiendabloquecard_origen UNIQUE (bloque_id, origen_legacy)
);

CREATE INDEX idx_tiendabloquecard_bloque_orden ON tienda_bloque_card (bloque_id, orden);
