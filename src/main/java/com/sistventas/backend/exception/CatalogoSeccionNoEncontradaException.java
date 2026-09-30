package com.sistventas.backend.exception;

public class CatalogoSeccionNoEncontradaException extends RuntimeException {
    public CatalogoSeccionNoEncontradaException() {
        super("Sección del catálogo no encontrada");
    }
}
