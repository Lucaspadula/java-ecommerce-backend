package com.sistventas.backend.dto;

import com.sistventas.backend.entity.EstadoVenta;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VentaResumenDto(
        Long id,
        String clienteNombre,
        LocalDate fechaEntrega,
        BigDecimal total,
        EstadoVenta estado
) {}
