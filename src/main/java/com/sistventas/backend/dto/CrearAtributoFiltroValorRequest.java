package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearAtributoFiltroValorRequest(
        @NotBlank(message = "El valor es obligatorio")
        @Size(max = 100, message = "El valor no puede superar los 100 caracteres")
        String valor
) {}
