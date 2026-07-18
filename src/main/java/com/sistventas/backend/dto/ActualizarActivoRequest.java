package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotNull;

public record ActualizarActivoRequest(
        @NotNull(message = "El campo activo es obligatorio")
        Boolean activo
) {}
