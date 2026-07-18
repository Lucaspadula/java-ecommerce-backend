package com.sistventas.backend.dto;

import com.sistventas.backend.entity.EstadoVenta;

import java.time.LocalDateTime;

public record VentaEstadoHistorialDto(
        Long id,
        EstadoVenta estadoAnterior,
        EstadoVenta estadoNuevo,
        String usuarioNombre,
        LocalDateTime fecha
) {}
