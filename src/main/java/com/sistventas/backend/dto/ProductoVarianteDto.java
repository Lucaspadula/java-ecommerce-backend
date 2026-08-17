package com.sistventas.backend.dto;

// Variante de color de un producto, con su propio stock. fotoUrl nullable:
// el admin la carga aparte con /api/productos/{id}/variantes/{varianteId}/foto,
// puede no tener una todavía. Sin precioVenta a propósito — ver
// ProductoVarianteRequest.
public record ProductoVarianteDto(
        Long id,
        String color,
        Integer stock,
        String fotoUrl
) {}
