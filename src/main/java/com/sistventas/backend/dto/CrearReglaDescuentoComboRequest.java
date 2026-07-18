package com.sistventas.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CrearReglaDescuentoComboRequest(
        @NotNull(message = "La categoría A es obligatoria")
        Long categoriaAId,

        // Opcional: sin subcategoría, el lado matchea cualquier producto de
        // esa categoría.
        Long subcategoriaAId,

        @NotNull(message = "La categoría B es obligatoria")
        Long categoriaBId,

        Long subcategoriaBId,

        @NotNull(message = "El porcentaje es obligatorio")
        @DecimalMin(value = "0.01", message = "El porcentaje debe ser mayor a 0")
        @DecimalMax(value = "100", message = "El porcentaje no puede superar 100")
        BigDecimal porcentaje,

        // Boxed (no boolean primitivo) a propósito: si el form viejo/un
        // cliente no manda el campo, null se interpreta como "activa" (ver
        // ReglaDescuentoComboServiceImpl.crear) en vez de silenciosamente
        // crear una regla inactiva por el default false del JSON.
        Boolean activo
) {}
