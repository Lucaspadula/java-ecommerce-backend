package com.sistventas.backend.dto;

// Ítem de subcategoría del mega-menú de categorías de la tienda pública
// (ver PublicTiendaServiceImpl.listarCategoriasMenu).
public record PublicSubcategoriaMenuDto(
        Long id,
        String nombre,
        String imagenUrl
) {}
