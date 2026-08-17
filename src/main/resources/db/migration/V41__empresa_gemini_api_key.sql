-- Key propia de Gemini por empresa (multi-tenant real: cada negocio usa su
-- propia cuota gratis de Google, no comparten la de Lucas). Nullable: si no
-- la cargó, DescripcionIaServiceImpl cae a sistventas.gemini.api-key (la
-- variable de entorno global) como fallback — ver Empresa.geminiApiKey.
ALTER TABLE empresa
    ADD COLUMN gemini_api_key VARCHAR(255) NULL;
