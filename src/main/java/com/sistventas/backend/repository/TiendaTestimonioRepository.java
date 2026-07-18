package com.sistventas.backend.repository;

import com.sistventas.backend.entity.TiendaTestimonio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TiendaTestimonioRepository extends JpaRepository<TiendaTestimonio, Long> {
    List<TiendaTestimonio> findByEmpresaIdOrderByOrdenAscIdAsc(Long empresaId);

    long countByEmpresaId(Long empresaId);

    // Usado antes de borrar: nunca se confía en un id que venga del cliente
    // sin validar que el testimonio sea de ESTA empresa, mismo criterio que
    // ResenaRepository.findByIdAndProductoIdAndEmpresaId.
    Optional<TiendaTestimonio> findByIdAndEmpresaId(Long id, Long empresaId);
}
