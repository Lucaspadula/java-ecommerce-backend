package com.sistventas.backend.dto;

import java.time.LocalDateTime;

public record ResenaDto(
        Long id,
        String clienteNombre,
        String comentario,
        LocalDateTime fecha,
        String imagenUrl
) {}
