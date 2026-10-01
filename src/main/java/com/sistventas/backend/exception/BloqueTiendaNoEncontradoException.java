package com.sistventas.backend.exception;

public class BloqueTiendaNoEncontradoException extends RuntimeException {
    public BloqueTiendaNoEncontradoException() {
        super("Bloque no encontrado");
    }
}
