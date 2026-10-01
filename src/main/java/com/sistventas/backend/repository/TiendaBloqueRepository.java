package com.sistventas.backend.repository;

import com.sistventas.backend.entity.TiendaBloque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TiendaBloqueRepository extends JpaRepository<TiendaBloque, Long> {

    List<TiendaBloque> findByEmpresaIdOrderBySlotAscOrdenAscIdAsc(Long empresaId);

    // Vidriera pública: solo los activos, agrupables por slot.
    List<TiendaBloque> findByEmpresaIdAndActivoTrueOrderBySlotAscOrdenAscIdAsc(Long empresaId);

    List<TiendaBloque> findByEmpresaIdAndSlotOrderByOrdenAscIdAsc(Long empresaId, String slot);

    long countByEmpresaId(Long empresaId);

    long countByEmpresaIdAndSlot(Long empresaId, String slot);

    // Nunca se confía en un id del cliente sin validar que el bloque sea de
    // ESTA empresa (mismo criterio que TiendaTipRepository).
    Optional<TiendaBloque> findByIdAndEmpresaId(Long id, Long empresaId);
}
