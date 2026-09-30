package com.sistventas.backend.dto;

// Subcategoría maestra, siempre hija de una Categoria (ver entity
// Subcategoria).
public record SubcategoriaDto(
        Long id,
        String nombre,
        String imagenUrl
) {}
