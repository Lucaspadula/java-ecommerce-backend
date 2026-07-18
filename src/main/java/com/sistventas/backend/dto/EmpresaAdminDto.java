package com.sistventas.backend.dto;

import com.sistventas.backend.entity.LicenciaEstado;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmpresaAdminDto(
        Long id,
        String nombre,
        LicenciaEstado licenciaEstado,
        LocalDate licenciaVencimiento,
        LocalDateTime fechaAlta,
        String adminNombre,
        String adminEmail
) {}
