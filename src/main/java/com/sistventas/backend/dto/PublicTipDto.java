package com.sistventas.backend.dto;

// Solo lectura, sin id ni orden (ya vienen ordenados en la lista): mismo
// criterio que PublicTestimonioDto.
public record PublicTipDto(
        String titulo,
        String contenido,
        String fotoUrl
) {}
