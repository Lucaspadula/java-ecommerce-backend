package com.sistventas.backend.exception;

public class SlugEnUsoException extends RuntimeException {
    public SlugEnUsoException() {
        super("Ese identificador de tienda ya está en uso");
    }
}
