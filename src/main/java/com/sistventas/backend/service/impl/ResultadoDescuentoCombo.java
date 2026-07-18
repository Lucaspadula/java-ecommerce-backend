package com.sistventas.backend.service.impl;

import com.sistventas.backend.entity.ReglaDescuentoCombo;

import java.math.BigDecimal;

// Salida del motor de cálculo: qué regla ganó (null si ninguna aplica),
// cuántos pares completos se formaron y el monto total de descuento.
public record ResultadoDescuentoCombo(
        ReglaDescuentoCombo reglaAplicada,
        int pares,
        BigDecimal montoDescuento,
        // Snapshot legible ("Mates + Bombillas"), se guarda tal cual en
        // Venta.descuentoComboDetalle para no depender de que la regla siga
        // existiendo/sin editar después.
        String detalle
) {
    public static ResultadoDescuentoCombo sinDescuento() {
        return new ResultadoDescuentoCombo(null, 0, BigDecimal.ZERO, null);
    }
}
