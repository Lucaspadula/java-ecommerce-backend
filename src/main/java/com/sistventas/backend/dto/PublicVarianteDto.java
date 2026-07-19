package com.sistventas.backend.dto;

// Variante de color de un producto, vista pública (tienda). `disponible` ya
// viene resuelto (StockDisponibleCalculator.calcularVariante) — el frontend
// nunca calcula stock, solo lo muestra.
public record PublicVarianteDto(
        Long id,
        String color,
        int disponible
) {}
