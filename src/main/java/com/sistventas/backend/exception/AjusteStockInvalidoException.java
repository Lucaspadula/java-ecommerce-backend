package com.sistventas.backend.exception;

public class AjusteStockInvalidoException extends RuntimeException {
    public AjusteStockInvalidoException(String nombreProducto, int stockActual, int delta) {
        super("El ajuste dejaría el stock en negativo para \"" + nombreProducto + "\": actual "
                + stockActual + ", delta " + delta);
    }
}
