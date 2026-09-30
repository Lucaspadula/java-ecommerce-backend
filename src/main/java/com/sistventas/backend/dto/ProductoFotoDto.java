package com.sistventas.backend.dto;

// Una foto del pool unificado (hasta 8 por producto, ver spec "Tope de pool
// por producto"). varianteId null = foto "general" (sin color), no atada a
// ninguna variante puntual.
public record ProductoFotoDto(
        Long id,
        String url,
        Long varianteId,
        int orden,
        // Ajuste manual del dueño: false = se ve completa (contain), true =
        // se agranda para llenar el marco aunque recorte bordes (cover).
        boolean agrandada
) {}
