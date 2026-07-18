package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActualizarColorCategoriaRequest(
        @NotBlank(message = "El color es obligatorio")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "El color debe ser un hexadecimal de 6 dígitos, ej. #a97d74")
        String color
) {}
