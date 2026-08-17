package com.sistventas.backend.exception;

public class IaGeneracionFallidaException extends RuntimeException {
    public IaGeneracionFallidaException() {
        super("No se pudo generar la descripción con IA. Intentá de nuevo.");
    }
}
