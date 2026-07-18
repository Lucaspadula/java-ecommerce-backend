package com.sistventas.backend.dto;

import com.sistventas.backend.entity.EstadoVenta;

public record EstadoConteoDto(
        EstadoVenta estado,
        long cantidad
) {}
