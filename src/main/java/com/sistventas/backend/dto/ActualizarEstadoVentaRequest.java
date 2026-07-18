package com.sistventas.backend.dto;

import com.sistventas.backend.entity.EstadoVenta;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoVentaRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoVenta estado
) {}
