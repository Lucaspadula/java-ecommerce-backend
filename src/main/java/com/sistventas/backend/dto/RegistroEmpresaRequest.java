package com.sistventas.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroEmpresaRequest(
        @NotBlank(message = "El nombre de la empresa es obligatorio")
        String nombreEmpresa,

        @NotBlank(message = "El nombre de usuario es obligatorio")
        String nombreUsuario,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        // Mismo criterio en los 4 DTOs que reciben contraseña nueva (ver
        // frontend core/password-strength): al menos un número y un símbolo,
        // sin esto el form del frontend ni siquiera llega a mandar el POST,
        // pero el backend es la barrera real.
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[^A-Za-z0-9]).*$", message = "La contraseña debe incluir al menos un número y un símbolo")
        String password
) {}
