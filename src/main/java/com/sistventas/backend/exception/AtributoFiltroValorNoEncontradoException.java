package com.sistventas.backend.exception;

public class AtributoFiltroValorNoEncontradoException extends RuntimeException {
    public AtributoFiltroValorNoEncontradoException() {
        super("Valor de atributo no encontrado");
    }
}
