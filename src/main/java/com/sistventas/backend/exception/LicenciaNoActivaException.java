package com.sistventas.backend.exception;

public class LicenciaNoActivaException extends RuntimeException {
    public LicenciaNoActivaException(String mensaje) {
        super(mensaje);
    }
}
