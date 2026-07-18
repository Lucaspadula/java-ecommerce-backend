package com.sistventas.backend.dto;

// Variante de color de un producto, con su propio stock. fotoUrl nullable:
// recién se usa en la tienda pública (Etapa 2), acá solo se expone si el
// admin ya la cargó.
public record ProductoVarianteDto(
        Long id,
        String color,
        Integer stock,
        String fotoUrl
) {}
