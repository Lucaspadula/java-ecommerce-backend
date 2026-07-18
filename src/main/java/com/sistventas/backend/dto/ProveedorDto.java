package com.sistventas.backend.dto;

import com.sistventas.backend.entity.EstadoPedido;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProveedorDto(
        Long id,
        String nombre,
        String contacto,
        String notas,
        EstadoPedido estadoPedido,
        String detallePedidoActual,
        LocalDate fechaPedido,
        LocalDate fechaLlegadaEstimada,
        String ultimoPedidoDetalle,
        LocalDateTime fechaAlta
) {}
