package com.sistventas.backend.exception;

public class BannerImagenNoEncontradaException extends RuntimeException {
    public BannerImagenNoEncontradaException() {
        super("Imagen de banner no encontrada");
    }
}
