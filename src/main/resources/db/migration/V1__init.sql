-- Fundación multi-tenant: empresa (tenant) y usuario

CREATE TABLE empresa (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre                VARCHAR(150)    NOT NULL,
    licencia_estado       VARCHAR(20)     NOT NULL DEFAULT 'PENDIENTE',
    licencia_vencimiento  DATE            NULL,
    fecha_alta            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_licencia_estado
        CHECK (licencia_estado IN ('PENDIENTE', 'ACTIVA', 'SUSPENDIDA', 'VENCIDA'))
);

CREATE TABLE usuario (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id       BIGINT          NULL,
    nombre           VARCHAR(150)    NOT NULL,
    email            VARCHAR(190)    NOT NULL,
    password_hash    VARCHAR(255)    NOT NULL,
    rol_empresa      VARCHAR(20)     NULL,
    es_super_admin   BOOLEAN         NOT NULL DEFAULT FALSE,
    activo           BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_alta       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT fk_usuario_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT chk_rol_empresa
        CHECK (rol_empresa IS NULL OR rol_empresa IN ('ADMIN', 'MEMBER'))
);

CREATE INDEX idx_usuario_empresa ON usuario (empresa_id);
