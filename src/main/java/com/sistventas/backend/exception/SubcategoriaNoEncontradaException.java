package com.sistventas.backend.exception;

public class SubcategoriaNoEncontradaException extends RuntimeException {
    public SubcategoriaNoEncontradaException() {
        super("Subcategoría no encontrada");
    }
}
