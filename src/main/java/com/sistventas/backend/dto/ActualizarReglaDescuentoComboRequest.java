package com.sistventas.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// A diferencia de crear, acá sí viaja `activo`: el form de edición del panel
// reusa los mismos campos + el checkbox de activo/inactivo.
public record ActualizarReglaDescuentoComboRequest(
        @NotNull(message = "La categoría A es obligatoria")
        Long categoriaAId,

        Long subcategoriaAId,

        @NotNull(message = "La categoría B es obligatoria")
        Long categoriaBId,

        Long subcategoriaBId,

        @NotNull(message = "El porcentaje es obligatorio")
        @DecimalMin(value = "0.01", message = "El porcentaje debe ser mayor a 0")
        @DecimalMax(value = "100", message = "El porcentaje no puede superar 100")
        BigDecimal porcentaje,

        boolean activo
) {}
