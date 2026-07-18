package com.sistventas.backend.exception;

public class UsuarioNoEncontradoException extends RuntimeException {
    public UsuarioNoEncontradoException() {
        super("Usuario no encontrado");
    }
}
