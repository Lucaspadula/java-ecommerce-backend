package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearTipRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede superar los 150 caracteres")
        String titulo,

        @NotBlank(message = "El contenido es obligatorio")
        @Size(max = 500, message = "El contenido no puede superar los 500 caracteres")
        String contenido
) {}
