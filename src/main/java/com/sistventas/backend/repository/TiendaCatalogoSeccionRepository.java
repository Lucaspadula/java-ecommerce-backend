package com.sistventas.backend.repository;

import com.sistventas.backend.entity.TiendaCatalogoSeccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TiendaCatalogoSeccionRepository extends JpaRepository<TiendaCatalogoSeccion, Long> {
    List<TiendaCatalogoSeccion> findByEmpresaIdOrderByTipoAscOrdenAscIdAsc(Long empresaId);

    List<TiendaCatalogoSeccion> findByEmpresaIdAndTipoOrderByOrdenAscIdAsc(Long empresaId, String tipo);

    long countByEmpresaIdAndTipo(Long empresaId, String tipo);

    // Usado antes de borrar: nunca se confía en un id que venga del cliente
    // sin validar que la sección sea de ESTA empresa, mismo criterio que
    // TiendaTipRepository.findByIdAndEmpresaId.
    Optional<TiendaCatalogoSeccion> findByIdAndEmpresaId(Long id, Long empresaId);
}
