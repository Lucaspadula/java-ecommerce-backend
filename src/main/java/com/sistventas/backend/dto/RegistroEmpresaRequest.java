package com.sistventas.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
        String password
) {}
