package com.sistventas.backend.dto;

import com.sistventas.backend.entity.RolEmpresa;

public record UsuarioLoginDto(
        Long id,
        String nombre,
        String email,
        boolean esSuperAdmin,
        RolEmpresa rolEmpresa,
        Long empresaId,
        String empresaNombre,
        String empresaLogoUrl
) {}
