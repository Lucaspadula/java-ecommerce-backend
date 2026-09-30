package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ClienteLoginRequest(
        @NotBlank(message = "El email es obligatorio")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}
