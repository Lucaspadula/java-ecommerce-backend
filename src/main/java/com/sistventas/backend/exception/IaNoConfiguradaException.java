package com.sistventas.backend.exception;

public class IaNoConfiguradaException extends RuntimeException {
    public IaNoConfiguradaException() {
        super("La generación de descripción con IA todavía no está configurada.");
    }
}
