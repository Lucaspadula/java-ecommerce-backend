package com.sistventas.backend.dto;

import java.util.List;

public record TiendaBloqueDto(
        Long id,
        String titulo,
        String slot,
        String ancho,
        Integer orden,
        Boolean activo,
        List<TiendaBloqueCardDto> cards
) {}
