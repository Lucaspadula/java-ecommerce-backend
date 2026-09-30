package com.sistventas.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrarClienteRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        // Antes mínimo 6 — se unificó a 8 con el resto de la app (ver
        // política única en frontend core/password-strength).
        @Size(min = 8, message = "La contraseña tiene que tener al menos 8 caracteres")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[^A-Za-z0-9]).*$", message = "La contraseña debe incluir al menos un número y un símbolo")
        String password,

        @NotBlank(message = "El teléfono es obligatorio")
        String telefono
) {}
