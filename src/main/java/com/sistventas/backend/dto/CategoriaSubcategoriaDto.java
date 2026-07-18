package com.sistventas.backend.dto;

import java.util.List;

// Para poblar los selectors en cascada (categoría -> subcategoría) del form
// de reglas de descuento combo, a partir del catálogo maestro de la empresa
// (ver entities Categoria/Subcategoria y
// ReglaDescuentoComboServiceImpl.listarCategoriasSubcategorias).
public record CategoriaSubcategoriaDto(
        Long categoriaId,
        String categoria,
        List<SubcategoriaDto> subcategorias
) {}
