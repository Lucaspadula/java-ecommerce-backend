package com.sistventas.backend.service;

import com.sistventas.backend.dto.CategoriaDto;
import com.sistventas.backend.dto.CrearCategoriaRequest;
import com.sistventas.backend.dto.CrearSubcategoriaRequest;
import com.sistventas.backend.dto.SubcategoriaDto;
import com.sistventas.backend.security.UserPrincipal;

import java.util.List;

// Catálogo maestro de categorías/subcategorías de producto (ver entities
// Categoria/Subcategoria). Abierto a cualquier usuario autenticado de la
// empresa (no admin-only): mismo criterio que ProductoService, gestionar el
// catálogo de productos no es una acción exclusiva de ADMIN.
public interface CategoriaService {
    List<CategoriaDto> listar(UserPrincipal principal);

    // Find-or-create: si ya existe una categoría con ese nombre
    // (case-insensitive) para la empresa, la devuelve en vez de duplicarla.
    // Habilita el flujo de "+ Crear categoría nueva" inline del form de
    // Productos sin arriesgar duplicados por mayúsculas/espacios.
    CategoriaDto crear(CrearCategoriaRequest request, UserPrincipal principal);

    List<SubcategoriaDto> listarSubcategorias(Long categoriaId, UserPrincipal principal);

    SubcategoriaDto crearSubcategoria(Long categoriaId, CrearSubcategoriaRequest request, UserPrincipal principal);
}
