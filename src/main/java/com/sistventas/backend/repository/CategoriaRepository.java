package com.sistventas.backend.repository;

import com.sistventas.backend.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByEmpresaIdOrderByNombreAsc(Long empresaId);

    // Usado antes de editar/borrar/referenciar: nunca se confía en un id que
    // venga del cliente sin validar que la categoría sea de ESTA empresa,
    // mismo criterio que ProductoRepository.findByIdAndEmpresaId.
    Optional<Categoria> findByIdAndEmpresaId(Long id, Long empresaId);

    // Find-or-create en CategoriaServiceImpl: evita duplicados por
    // mayúsculas/espacios ("Mates" vs "mates" vs " Mates ") cuando dos
    // productos/usuarios distintos escriben la "misma" categoría nueva.
    Optional<Categoria> findByEmpresaIdAndNombreIgnoreCase(Long empresaId, String nombre);
}
