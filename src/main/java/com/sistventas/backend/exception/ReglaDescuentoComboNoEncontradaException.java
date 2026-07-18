package com.sistventas.backend.exception;

public class ReglaDescuentoComboNoEncontradaException extends RuntimeException {
    public ReglaDescuentoComboNoEncontradaException() {
        super("Regla de descuento no encontrada");
    }
}
