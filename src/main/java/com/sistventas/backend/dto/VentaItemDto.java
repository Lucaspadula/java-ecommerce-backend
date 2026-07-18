package com.sistventas.backend.dto;

import java.math.BigDecimal;

public record VentaItemDto(
        Long id,
        Long productoId,
        String productoNombre,
        int cantidad,
        BigDecimal precioUnitario,
        String personalizacion,
        BigDecimal subtotal,
        String fotoUrl,
        Long varianteId
) {}
