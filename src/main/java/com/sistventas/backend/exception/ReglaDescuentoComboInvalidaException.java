package com.sistventas.backend.exception;

// Lanzada cuando los dos lados de la regla matchean exactamente lo mismo
// (misma categoría + misma subcategoría): un producto no puede "parearse"
// consigo mismo, ver ReglaDescuentoComboServiceImpl.validarLadosDistintos.
public class ReglaDescuentoComboInvalidaException extends RuntimeException {
    public ReglaDescuentoComboInvalidaException() {
        super("Los dos lados de la regla deben ser distintos");
    }
}
