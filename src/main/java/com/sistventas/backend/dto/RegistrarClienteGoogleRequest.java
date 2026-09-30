package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;

// El idToken de Google no trae teléfono (ver GoogleTokenVerifier) — se pide
// en un paso extra del form de registro, ya que es la clave que vincula la
// cuenta nueva con compras de invitado anteriores.
public record RegistrarClienteGoogleRequest(
        @NotBlank(message = "El token de Google es obligatorio")
        String idToken,

        @NotBlank(message = "El teléfono es obligatorio")
        String telefono
) {}
