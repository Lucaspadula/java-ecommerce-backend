package com.sistventas.backend.exception;

public class TipNoEncontradoException extends RuntimeException {
    public TipNoEncontradoException() {
        super("Tip no encontrado");
    }
}
