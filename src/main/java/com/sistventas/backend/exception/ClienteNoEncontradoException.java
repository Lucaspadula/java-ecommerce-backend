package com.sistventas.backend.exception;

public class ClienteNoEncontradoException extends RuntimeException {
    public ClienteNoEncontradoException() {
        super("Cliente no encontrado");
    }
}
