package com.sistventas.backend.exception;

public class ProductoFotoNoEncontradaException extends RuntimeException {
    public ProductoFotoNoEncontradaException() {
        super("Foto no encontrada");
    }
}
