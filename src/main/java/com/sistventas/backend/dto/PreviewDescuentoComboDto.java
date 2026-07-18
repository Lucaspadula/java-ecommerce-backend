package com.sistventas.backend.dto;

import java.math.BigDecimal;

// Preview del descuento combo para el carrito de la tienda pública, ANTES de
// confirmar el pedido. Es solo informativo: el monto final que se persiste
// en la Venta sale de un recálculo server-side idéntico en crearPedido,
// nunca de lo que muestra este preview (ver PublicTiendaServiceImpl).
public record PreviewDescuentoComboDto(
        boolean aplica,
        String detalle,
        Integer pares,
        BigDecimal porcentaje,
        BigDecimal montoDescuento
) {
    public static PreviewDescuentoComboDto sinDescuento() {
        return new PreviewDescuentoComboDto(false, null, 0, null, BigDecimal.ZERO);
    }
}
