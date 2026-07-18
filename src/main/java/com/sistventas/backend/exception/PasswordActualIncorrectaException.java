package com.sistventas.backend.exception;

public class PasswordActualIncorrectaException extends RuntimeException {
    public PasswordActualIncorrectaException() {
        super("La contraseña actual es incorrecta");
    }
}
