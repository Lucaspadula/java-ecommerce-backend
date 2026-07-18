package com.sistventas.backend.exception;

public class EmailYaRegistradoException extends RuntimeException {
    public EmailYaRegistradoException() {
        super("Ese email ya está registrado");
    }
}
