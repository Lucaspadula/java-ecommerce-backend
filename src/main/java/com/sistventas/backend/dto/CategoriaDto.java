package com.sistventas.backend.dto;

// Categoría maestra de producto (ver entity Categoria), para poblar el
// <select> real de Productos y el resto de las pantallas que antes usaban
// texto libre.
public record CategoriaDto(
        Long id,
        String nombre
) {}
