package com.sistventas.backend.repository;

import com.sistventas.backend.entity.TiendaTip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TiendaTipRepository extends JpaRepository<TiendaTip, Long> {
    List<TiendaTip> findByEmpresaIdOrderByOrdenAscIdAsc(Long empresaId);

    long countByEmpresaId(Long empresaId);

    // Usado antes de borrar: nunca se confía en un id que venga del cliente
    // sin validar que el tip sea de ESTA empresa, mismo criterio que
    // TiendaTestimonioRepository.findByIdAndEmpresaId.
    Optional<TiendaTip> findByIdAndEmpresaId(Long id, Long empresaId);
}
