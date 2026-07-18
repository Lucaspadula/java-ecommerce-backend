package com.sistventas.backend.exception;

public class ProveedorNoEncontradoException extends RuntimeException {
    public ProveedorNoEncontradoException() {
        super("Proveedor no encontrado");
    }
}
