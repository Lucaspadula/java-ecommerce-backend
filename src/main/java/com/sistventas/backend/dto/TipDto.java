package com.sistventas.backend.dto;

// DTO admin (ver PublicTipDto para la versión pública, sin id/orden).
public record TipDto(
        Long id,
        String titulo,
        String contenido,
        Integer orden,
        String fotoUrl
) {}
