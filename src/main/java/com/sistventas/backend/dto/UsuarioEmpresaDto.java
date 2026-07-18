package com.sistventas.backend.dto;

import com.sistventas.backend.entity.RolEmpresa;

import java.time.LocalDateTime;

public record UsuarioEmpresaDto(
        Long id,
        String nombre,
        String email,
        RolEmpresa rolEmpresa,
        boolean activo,
        LocalDateTime fechaAlta
) {}
