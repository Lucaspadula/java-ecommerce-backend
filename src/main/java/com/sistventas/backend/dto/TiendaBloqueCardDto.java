package com.sistventas.backend.dto;

public record TiendaBloqueCardDto(
        Long id,
        String imagenUrl,
        String orientacion,
        String titulo,
        String texto,
        Integer orden,
        String accion,
        String accionValor
) {}
