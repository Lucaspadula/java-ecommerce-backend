package com.sistventas.backend.exception;

public class TestimonioNoEncontradoException extends RuntimeException {
    public TestimonioNoEncontradoException() {
        super("Testimonio no encontrado");
    }
}
