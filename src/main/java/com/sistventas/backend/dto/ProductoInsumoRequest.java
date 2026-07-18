package com.sistventas.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Línea de receta enviada al crear/editar un producto: referencia a un
// Insumo maestro (por id, scopeado a la empresa del usuario en el service) +
// cantidad usada por unidad de producto.
public record ProductoInsumoRequest(
        @NotNull(message = "El insumo es obligatorio")
        Long insumoId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a cero")
        BigDecimal cantidad
) {}
