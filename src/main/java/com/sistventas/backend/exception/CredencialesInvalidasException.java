package com.sistventas.backend.exception;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Credenciales inválidas");
    }
}
