-- Cartera de clientes por empresa. Mismo criterio de scoping multiempresa
-- que producto: empresa_id como FK plana, sin necesidad de navegar de
-- Cliente a Empresa en esta feature.

CREATE TABLE cliente (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id   BIGINT NOT NULL,
    nombre       VARCHAR(150) NOT NULL,
    email        VARCHAR(190) NULL,
    telefono     VARCHAR(40) NULL,
    notas        TEXT NULL,
    activo       BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_alta   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cliente_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

CREATE INDEX idx_cliente_empresa ON cliente (empresa_id);
