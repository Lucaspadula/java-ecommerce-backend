package com.sistventas.backend.dto;

import java.util.List;

// Árbol categoría -> subcategorías para el mega-menú del navbar de la
// tienda pública (ver PublicTiendaServiceImpl.listarCategoriasMenu). Solo
// incluye categorías/subcategorías con al menos un producto activo — no
// tiene sentido un link de menú a una sección vacía. imagenUrl sale de
// TiendaCategoria (la misma que usa el carrusel "Explorá por categoría").
public record PublicCategoriaMenuDto(
        Long categoriaId,
        String nombre,
        String imagenUrl,
        List<PublicSubcategoriaMenuDto> subcategorias
) {}
