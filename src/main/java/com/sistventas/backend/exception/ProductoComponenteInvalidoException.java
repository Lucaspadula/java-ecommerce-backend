package com.sistventas.backend.exception;

// Cubre los tres casos inválidos de armar un kit (ver
// ProductoServiceImpl.aplicarComponentes): un producto no puede tenerse a sí
// mismo como componente, un componente no puede ser a su vez un kit (el
// cálculo de stock no es recursivo, ver StockDisponibleCalculator), y todo
// producto necesita al menos un insumo o un componente propio.
public class ProductoComponenteInvalidoException extends RuntimeException {
    public ProductoComponenteInvalidoException(String mensaje) {
        super(mensaje);
    }
}
