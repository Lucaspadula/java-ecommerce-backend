package com.sistventas.backend.exception;

public class AccionNoPermitidaException extends RuntimeException {
    public AccionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
