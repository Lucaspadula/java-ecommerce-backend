package com.sistventas.backend.exception;

public class TiendaNoEncontradaException extends RuntimeException {
    public TiendaNoEncontradaException() {
        super("Tienda no encontrada");
    }
}
