package com.sistventas.backend.dto;

// Variante de color de un producto, con su propio stock. fotoUrl nullable:
// derivado (ProductoFotoResolver.fotosDeVariante sobre el pool, primera por
// orden), no una columna propia — ver design Decision #4. Sin precioVenta a
// propósito — ver ProductoVarianteRequest.
public record ProductoVarianteDto(
        Long id,
        String color,
        Integer stock,
        String fotoUrl,
        // Hex real del color (nullable: variantes viejas no lo tienen
        // cargado todavía), ver ProductoVariante.colorHex.
        String colorHex
) {}
