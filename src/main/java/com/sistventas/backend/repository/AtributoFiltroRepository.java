package com.sistventas.backend.repository;

import com.sistventas.backend.entity.AtributoFiltro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AtributoFiltroRepository extends JpaRepository<AtributoFiltro, Long> {
    List<AtributoFiltro> findByCategoriaIdOrderByOrdenAsc(Long categoriaId);

    // Usado antes de crear valores/editar/borrar: valida que el atributo sea
    // de ESA categoría (y de paso, indirectamente, de la empresa dueña de esa
    // categoría) — mismo criterio que Subcategoria.findByIdAndCategoriaId.
    Optional<AtributoFiltro> findByIdAndCategoriaId(Long id, Long categoriaId);

    Optional<AtributoFiltro> findByCategoriaIdAndNombreIgnoreCase(Long categoriaId, String nombre);

    List<AtributoFiltro> findByEmpresaIdAndCategoriaIdIn(Long empresaId, List<Long> categoriaIds);
}
