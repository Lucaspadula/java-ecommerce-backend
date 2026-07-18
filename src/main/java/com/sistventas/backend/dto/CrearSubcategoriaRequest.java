package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Find-or-create, mismo criterio que CrearCategoriaRequest.
public record CrearSubcategoriaRequest(
        @NotBlank(message = "El nombre de la subcategoría es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre
) {}
