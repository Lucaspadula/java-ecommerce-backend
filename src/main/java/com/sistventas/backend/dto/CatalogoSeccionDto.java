package com.sistventas.backend.dto;

public record CatalogoSeccionDto(
        Long id,
        String tipo,
        String texto,
        Integer orden,
        String alineacion,
        boolean negrita,
        boolean cursiva,
        boolean subrayado
) {}
