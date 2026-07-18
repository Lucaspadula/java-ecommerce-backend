package com.sistventas.backend.exception;

public class CuponInvalidoException extends RuntimeException {
    public CuponInvalidoException() {
        super("El código de descuento no es válido");
    }
}
