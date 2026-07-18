package com.sistventas.backend.dto;

// Versión pública (sin `orden` ni `tipo`, que son detalle de administración)
// de una imagen de banner. productoId viaja para que el frontend público
// pueda abrir el modal de detalle del producto asociado al clickear el
// banner (null = el click no hace nada especial).
public record BannerImagenPublicaDto(
        String imagenUrl,
        Long productoId
) {}
