package com.sistventas.backend.exception;

public class InsumoNoEncontradoException extends RuntimeException {
    public InsumoNoEncontradoException() {
        super("Insumo no encontrado");
    }
}
