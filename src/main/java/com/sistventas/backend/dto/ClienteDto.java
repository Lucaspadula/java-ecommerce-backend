package com.sistventas.backend.dto;

import java.time.LocalDateTime;

public record ClienteDto(
        Long id,
        String nombre,
        String email,
        String telefono,
        String notas,
        LocalDateTime fechaAlta
) {}
