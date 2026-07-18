package com.sistventas.backend.dto;

public record BannerImagenTiendaDto(
        Long id,
        String imagenUrl,
        int orden,
        String tipo,
        Long productoId
) {}
