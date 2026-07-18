package com.sistventas.backend.exception;

// Se lanza cuando un usuario autenticado y con empresa intenta realizar una
// acción reservada al ADMIN (gestionar usuarios, subir el logo, etc.) sin
// tener ese rol (ej. rol MEMBER). Distinta de SinEmpresaException: acá el
// usuario sí pertenece a una empresa, pero no tiene el rol necesario.
public class AccesoRestringidoAdminException extends RuntimeException {
    public AccesoRestringidoAdminException() {
        super("Solo el administrador de la empresa puede realizar esta acción");
    }
}
