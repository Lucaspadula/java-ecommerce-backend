package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String passwordActual,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[^A-Za-z0-9]).*$", message = "La nueva contraseña debe incluir al menos un número y un símbolo")
        String passwordNueva
) {}
