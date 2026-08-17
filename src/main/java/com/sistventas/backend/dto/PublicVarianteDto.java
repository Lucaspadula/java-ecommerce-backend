package com.sistventas.backend.dto;

// Variante de color de un producto, vista pública (tienda). `disponible` ya
// viene resuelto (StockDisponibleCalculator.calcularVariante) — el frontend
// nunca calcula stock, solo lo muestra. Sin precio propio: todas las
// variantes de un producto cobran siempre el precioVenta del Producto (ver
// ProductoVarianteRequest — el override por color se sacó a propósito).
public record PublicVarianteDto(
        Long id,
        String color,
        int disponible
) {}
