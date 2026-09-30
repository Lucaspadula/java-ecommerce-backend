package com.sistventas.backend.service;

import com.sistventas.backend.dto.AtributoFiltroDto;
import com.sistventas.backend.dto.CategoriaDto;
import com.sistventas.backend.dto.CrearAtributoFiltroRequest;
import com.sistventas.backend.dto.CrearAtributoFiltroValorRequest;
import com.sistventas.backend.dto.CrearCategoriaRequest;
import com.sistventas.backend.dto.CrearSubcategoriaRequest;
import com.sistventas.backend.dto.SubcategoriaDto;
import com.sistventas.backend.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;

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

    // Admin-only (a diferencia del resto de esta interfaz): mismo criterio
    // que TiendaCategoriaService.actualizarImagen — es personalización
    // visual de la vidriera pública, no gestión del catálogo de productos.
    SubcategoriaDto actualizarImagenSubcategoria(Long categoriaId, Long subcategoriaId, MultipartFile file, UserPrincipal principal);

    SubcategoriaDto eliminarImagenSubcategoria(Long categoriaId, Long subcategoriaId, UserPrincipal principal);

    // --- Atributos de filtro (por categoría) ---
    List<AtributoFiltroDto> listarAtributosFiltro(Long categoriaId, UserPrincipal principal);

    AtributoFiltroDto crearAtributoFiltro(Long categoriaId, CrearAtributoFiltroRequest request, UserPrincipal principal);

    void eliminarAtributoFiltro(Long categoriaId, Long atributoId, UserPrincipal principal);

    AtributoFiltroDto crearValorAtributoFiltro(
            Long categoriaId, Long atributoId, CrearAtributoFiltroValorRequest request, UserPrincipal principal);

    void eliminarValorAtributoFiltro(Long categoriaId, Long atributoId, Long valorId, UserPrincipal principal);
}
