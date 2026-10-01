package com.sistventas.backend.dto;

import java.util.List;

public record PublicTiendaBloqueDto(
        Long id,
        String titulo,
        String slot,
        String ancho,
        List<PublicTiendaBloqueCardDto> cards
) {}
