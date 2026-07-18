package com.sistventas.backend.repository;

import com.sistventas.backend.entity.Subcategoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubcategoriaRepository extends JpaRepository<Subcategoria, Long> {
    List<Subcategoria> findByCategoriaIdOrderByNombreAsc(Long categoriaId);

    // Usado antes de asociarla a un Producto/regla: nunca se confía en un id
    // que venga del cliente sin validar que la subcategoría sea hija de LA
    // categoría elegida (evita mezclar subcategorías de otra categoría, u
    // otra empresa vía una categoriaId ajena).
    Optional<Subcategoria> findByIdAndCategoriaId(Long id, Long categoriaId);

    // Find-or-create en CategoriaServiceImpl, mismo criterio que
    // CategoriaRepository.findByEmpresaIdAndNombreIgnoreCase.
    Optional<Subcategoria> findByCategoriaIdAndNombreIgnoreCase(Long categoriaId, String nombre);
}
