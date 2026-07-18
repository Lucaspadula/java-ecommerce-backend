package com.sistventas.backend.dto;

import com.sistventas.backend.entity.EstadoVenta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PublicPedidoEstadoDto(
        Long ventaId,
        EstadoVenta estado,
        LocalDateTime fechaPedido,
        LocalDate fechaEntrega,
        BigDecimal total,
        List<PublicPedidoHistorialDto> historial
) {}
