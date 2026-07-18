package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.CategoriaDto;
import com.sistventas.backend.dto.CrearCategoriaRequest;
import com.sistventas.backend.dto.CrearSubcategoriaRequest;
import com.sistventas.backend.dto.SubcategoriaDto;
import com.sistventas.backend.entity.Categoria;
import com.sistventas.backend.entity.Subcategoria;
import com.sistventas.backend.exception.CategoriaNoEncontradaException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.CategoriaRepository;
import com.sistventas.backend.repository.SubcategoriaRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.CategoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaRepository subcategoriaRepository;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository, SubcategoriaRepository subcategoriaRepository) {
        this.categoriaRepository = categoriaRepository;
        this.subcategoriaRepository = subcategoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaDto> listar(UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        return categoriaRepository.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public CategoriaDto crear(CrearCategoriaRequest request, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        String nombre = request.nombre().trim();

        // Find-or-create (ver CrearCategoriaRequest): evita duplicados por
        // mayúsculas/espacios.
        Categoria categoria = categoriaRepository.findByEmpresaIdAndNombreIgnoreCase(empresaId, nombre)
                .orElseGet(() -> {
                    Categoria nueva = new Categoria();
                    nueva.setEmpresaId(empresaId);
                    nueva.setNombre(nombre);
                    return categoriaRepository.save(nueva);
                });
        return toDto(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubcategoriaDto> listarSubcategorias(Long categoriaId, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Categoria categoria = buscarCategoriaPorEmpresa(categoriaId, empresaId);
        return subcategoriaRepository.findByCategoriaIdOrderByNombreAsc(categoria.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public SubcategoriaDto crearSubcategoria(Long categoriaId, CrearSubcategoriaRequest request, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Categoria categoria = buscarCategoriaPorEmpresa(categoriaId, empresaId);
        String nombre = request.nombre().trim();

        Subcategoria subcategoria = subcategoriaRepository.findByCategoriaIdAndNombreIgnoreCase(categoria.getId(), nombre)
                .orElseGet(() -> {
                    Subcategoria nueva = new Subcategoria();
                    nueva.setCategoriaId(categoria.getId());
                    nueva.setNombre(nombre);
                    return subcategoriaRepository.save(nueva);
                });
        return toDto(subcategoria);
    }

    private Categoria buscarCategoriaPorEmpresa(Long categoriaId, Long empresaId) {
        return categoriaRepository.findByIdAndEmpresaId(categoriaId, empresaId)
                .orElseThrow(CategoriaNoEncontradaException::new);
    }

    private Long empresaIdOrThrow(UserPrincipal principal) {
        if (principal == null || principal.empresaId() == null) {
            throw new SinEmpresaException();
        }
        return principal.empresaId();
    }

    private CategoriaDto toDto(Categoria categoria) {
        return new CategoriaDto(categoria.getId(), categoria.getNombre());
    }

    private SubcategoriaDto toDto(Subcategoria subcategoria) {
        return new SubcategoriaDto(subcategoria.getId(), subcategoria.getNombre());
    }
}
