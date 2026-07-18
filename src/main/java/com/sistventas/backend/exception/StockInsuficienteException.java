package com.sistventas.backend.exception;

public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(String nombreProducto, int disponible, int solicitado) {
        super("Stock insuficiente para \"" + nombreProducto + "\": disponible " + disponible
                + ", solicitado " + solicitado);
    }
}
