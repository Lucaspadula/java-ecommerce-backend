package com.sistventas.backend.dto;

// Variante de color de un producto, vista pública (tienda). `disponible` ya
// viene resuelto (StockDisponibleCalculator.calcularVariante) — el frontend
// nunca calcula stock, solo lo muestra. Sin precio propio: todas las
// variantes de un producto cobran siempre el precioVenta del Producto (ver
// ProductoVarianteRequest — el override por color se sacó a propósito).
// fotoUrl nullable: derivado (ProductoFotoResolver.fotosDeVariante, primera
// por orden), no una columna propia. Si el color no tiene foto propia
// cargada, la tienda pública se queda con la foto principal del producto al
// seleccionarlo (ver seleccionarVarianteModal en tienda-publica.ts).
public record PublicVarianteDto(
        Long id,
        String color,
        int disponible,
        String fotoUrl,
        // Hex real del color (nullable, ver ProductoVariante.colorHex): la
        // tienda pública lo usa para pintar un swatch circular real en vez de
        // solo mostrar el nombre. Sin valor cargado, cae a un swatch gris
        // neutro con la inicial del nombre (ver tienda-producto.ts).
        String colorHex
) {}
