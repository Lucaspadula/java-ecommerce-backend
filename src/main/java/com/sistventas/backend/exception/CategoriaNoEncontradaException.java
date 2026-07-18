package com.sistventas.backend.exception;

public class CategoriaNoEncontradaException extends RuntimeException {
    public CategoriaNoEncontradaException() {
        super("Categoría no encontrada");
    }
}
