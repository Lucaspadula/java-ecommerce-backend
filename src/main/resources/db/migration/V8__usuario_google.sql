-- Login alternativo con Google Identity Services: la contraseña deja de ser
-- obligatoria (el login por Google no la usa) y se agrega el ID estable de
-- Google (sub) para vincular la cuenta al perfil de Google del usuario.

ALTER TABLE usuario MODIFY COLUMN password_hash VARCHAR(255) NULL;
ALTER TABLE usuario ADD COLUMN google_sub VARCHAR(255) NULL UNIQUE;
