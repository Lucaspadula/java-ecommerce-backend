package com.sistventas.backend.exception;

public class AtributoFiltroNoEncontradoException extends RuntimeException {
    public AtributoFiltroNoEncontradoException() {
        super("Atributo de filtro no encontrado");
    }
}
