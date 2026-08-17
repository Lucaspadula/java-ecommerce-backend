package com.sistventas.backend.dto;

// Línea de "qué incluye" un combo, para la vidriera pública: solo nombre y
// cantidad, nunca precio ni costo del componente por separado (el kit ya
// tiene su propio precioVenta único, ver PublicProductoDto).
public record PublicComponenteDto(
        String nombre,
        Integer cantidad
) {}
