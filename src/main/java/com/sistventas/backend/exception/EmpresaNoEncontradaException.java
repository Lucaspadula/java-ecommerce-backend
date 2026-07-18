package com.sistventas.backend.exception;

public class EmpresaNoEncontradaException extends RuntimeException {
    public EmpresaNoEncontradaException() {
        super("Empresa no encontrada");
    }
}
