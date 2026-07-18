package com.sistventas.backend.exception;

public class CuentaDeshabilitadaException extends RuntimeException {
    public CuentaDeshabilitadaException() {
        super("Tu cuenta está deshabilitada. Contactá al administrador.");
    }
}
