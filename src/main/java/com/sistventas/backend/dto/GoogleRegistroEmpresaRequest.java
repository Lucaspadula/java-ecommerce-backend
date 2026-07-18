package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleRegistroEmpresaRequest(
        @NotBlank(message = "El idToken es obligatorio")
        String idToken,

        @NotBlank(message = "El nombre de la empresa es obligatorio")
        String nombreEmpresa
) {}
