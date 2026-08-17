package com.sistventas.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClienteRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        String nombre,

        // @Email no falla con null/vacío (el campo sigue siendo opcional),
        // solo valida el formato cuando sí viene cargado.
        @Email(message = "El email no tiene un formato válido")
        String email,

        String telefono,

        String notas
) {}
