package com.sistventas.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// PUT /api/empresa/tienda/promociones: cupón y barra de oferta. Las reglas de
// combo tienen su propio CRUD (/tienda/descuentos-combo).
public record ActualizarPromocionesRequest(
        @Size(max = 40, message = "El código de cupón no puede superar los 40 caracteres")
        String tiendaCuponCodigo,

        @DecimalMin(value = "0.0", message = "El porcentaje del cupón no puede ser negativo")
        @DecimalMax(value = "100.0", message = "El porcentaje del cupón no puede superar 100")
        BigDecimal tiendaCuponPorcentaje,

        @NotNull(message = "El estado de la oferta es obligatorio")
        Boolean tiendaOfertaActiva,

        @Size(max = 60, message = "La etiqueta no puede superar los 60 caracteres")
        String tiendaOfertaEtiqueta,

        @Size(max = 150, message = "El texto de la oferta no puede superar los 150 caracteres")
        String tiendaOfertaTexto,

        LocalDateTime tiendaOfertaFechaFin
) {}
