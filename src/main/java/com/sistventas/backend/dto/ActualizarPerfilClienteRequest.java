package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Edición de "Mis datos" desde la tienda pública (cliente ya logueado) — a
// diferencia del registro, acá no hay email/password: el cliente edita
// nombre/teléfono, que es lo que se usa para autocompletar el checkout.
public record ActualizarPerfilClienteRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @NotBlank(message = "El teléfono es obligatorio")
        String telefono
) {}
