package com.sistventas.backend.exception;

// Se lanza cuando un usuario sin empresa (ej. Super Admin) intenta acceder a
// un endpoint que requiere estar scopeado a una empresa. Mapea a 403: el
// usuario está autenticado, simplemente no tiene permiso sobre este recurso.
public class SinEmpresaException extends RuntimeException {
    public SinEmpresaException() {
        super("El usuario no pertenece a ninguna empresa");
    }
}
