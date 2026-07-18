package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearResenaRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 150, message = "El nombre del cliente no puede superar los 150 caracteres")
        String clienteNombre,

        @NotBlank(message = "El comentario es obligatorio")
        @Size(max = 1000, message = "El comentario no puede superar los 1000 caracteres")
        String comentario
) {}
