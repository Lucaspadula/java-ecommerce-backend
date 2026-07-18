package com.sistventas.backend.exception;

public class ArchivoInvalidoException extends RuntimeException {
    public ArchivoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
