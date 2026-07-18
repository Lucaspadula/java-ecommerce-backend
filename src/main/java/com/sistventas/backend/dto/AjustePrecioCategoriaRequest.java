package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AjustePrecioCategoriaRequest(
        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        @NotNull(message = "El tipo de ajuste es obligatorio")
        TipoAjustePrecio tipoAjuste,

        // Positivo para aumento, negativo para descuento. En PORCENTAJE es un
        // porcentaje (ej. 10 = +10%, -5 = -5%); en MONTO_FIJO es un monto en
        // pesos a sumar/restar del precio actual.
        @NotNull(message = "El valor del ajuste es obligatorio")
        BigDecimal valor
) {}
