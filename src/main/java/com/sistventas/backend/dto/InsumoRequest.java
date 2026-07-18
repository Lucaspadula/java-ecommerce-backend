package com.sistventas.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Separado de InsumoDto a propósito: el insumo que viaja en el request de
// create/update no tiene id (lo genera el backend), mientras que el de la
// response sí. Evita un id nullable ambiguo compartido entre ambas direcciones.
public record InsumoRequest(
        @NotBlank(message = "El nombre del insumo es obligatorio")
        String nombre,

        @NotNull(message = "El costo del insumo es obligatorio")
        @DecimalMin(value = "0.0", message = "El costo no puede ser negativo")
        BigDecimal costoUnitario,

        @NotNull(message = "El stock del insumo es obligatorio")
        @DecimalMin(value = "0.0", message = "El stock no puede ser negativo")
        BigDecimal stock,

        BigDecimal stockMinimo,

        String unidadMedida
) {}
