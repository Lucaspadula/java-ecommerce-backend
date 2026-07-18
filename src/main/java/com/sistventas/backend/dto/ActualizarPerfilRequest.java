package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ActualizarPerfilRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre
) {}
