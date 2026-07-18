package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// precioUnitario es nullable a propósito: si no viene, el service toma el
// precioVenta actual del producto como default. Igual que InsumoRequest vs
// InsumoDto, este record separado de VentaItemDto evita un id nullable
// ambiguo compartido entre request y response.
public record VentaItemRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        Integer cantidad,

        BigDecimal precioUnitario,

        String personalizacion,

        // Url devuelta por POST /api/ventas/fotos, tal cual la manda el
        // frontend; null si el item no tiene foto de referencia.
        String fotoUrl
) {}
