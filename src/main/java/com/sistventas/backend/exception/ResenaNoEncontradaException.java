package com.sistventas.backend.exception;

public class ResenaNoEncontradaException extends RuntimeException {
    public ResenaNoEncontradaException() {
        super("Reseña no encontrada");
    }
}
