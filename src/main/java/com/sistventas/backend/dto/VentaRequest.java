package com.sistventas.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record VentaRequest(
        @NotNull(message = "El cliente es obligatorio")
        Long clienteId,

        LocalDate fechaEntrega,

        String notas,

        // Nullable: si no viene, se trata como BigDecimal.ZERO (sin descuento).
        BigDecimal descuentoPorcentaje,

        @NotEmpty(message = "La venta debe tener al menos un item")
        @Valid
        List<VentaItemRequest> items
) {}
