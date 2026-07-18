package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ClienteRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        String nombre,

        String email,

        String telefono,

        String notas
) {}
