package com.sistventas.backend.exception;

public class TokenGoogleInvalidoException extends RuntimeException {
    public TokenGoogleInvalidoException() {
        super("El token de Google no es válido");
    }
}
