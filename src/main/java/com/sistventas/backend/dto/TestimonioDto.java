package com.sistventas.backend.dto;

import com.sistventas.backend.entity.CanalTestimonio;

// DTO admin (ver PublicTestimonioDto para la versión pública, sin id/orden).
public record TestimonioDto(
        Long id,
        String clienteNombre,
        String comentario,
        Integer orden,
        String fotoUrl,
        CanalTestimonio canal
) {}
