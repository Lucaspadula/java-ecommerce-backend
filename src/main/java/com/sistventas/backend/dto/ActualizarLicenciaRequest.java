package com.sistventas.backend.dto;

import com.sistventas.backend.entity.LicenciaEstado;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ActualizarLicenciaRequest(
        @NotNull(message = "El estado es obligatorio")
        LicenciaEstado estado,
        LocalDate vencimiento
) {}
