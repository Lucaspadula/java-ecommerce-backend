package com.sistventas.backend.exception;

// Request de bloque/card con datos inválidos (slot, ancho, acción, URL,
// referencia ajena, límites). Se traduce a 400 en GlobalExceptionHandler.
public class BloqueTiendaInvalidoException extends RuntimeException {
    public BloqueTiendaInvalidoException(String mensaje) {
        super(mensaje);
    }
}
