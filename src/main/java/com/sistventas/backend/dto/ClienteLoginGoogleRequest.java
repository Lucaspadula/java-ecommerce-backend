package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ClienteLoginGoogleRequest(
        @NotBlank(message = "El token de Google es obligatorio")
        String idToken
) {}
