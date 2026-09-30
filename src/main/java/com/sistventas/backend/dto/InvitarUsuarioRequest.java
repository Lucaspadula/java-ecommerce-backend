package com.sistventas.backend.dto;

import com.sistventas.backend.entity.RolEmpresa;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InvitarUsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[^A-Za-z0-9]).*$", message = "La contraseña debe incluir al menos un número y un símbolo")
        String password,

        @NotNull(message = "El rol es obligatorio")
        RolEmpresa rolEmpresa
) {}
