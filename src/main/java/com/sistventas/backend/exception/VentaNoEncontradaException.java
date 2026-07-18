package com.sistventas.backend.exception;

public class VentaNoEncontradaException extends RuntimeException {
    public VentaNoEncontradaException() {
        super("Venta no encontrada");
    }
}
