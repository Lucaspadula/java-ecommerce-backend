package com.sistventas.backend.repository;

import com.sistventas.backend.entity.ReglaDescuentoCombo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReglaDescuentoComboRepository extends JpaRepository<ReglaDescuentoCombo, Long> {
    List<ReglaDescuentoCombo> findByEmpresaId(Long empresaId);

    // Usadas por el motor de cálculo (checkout público y preview): solo las
    // reglas activas de la empresa participan de "mejor regla gana".
    List<ReglaDescuentoCombo> findByEmpresaIdAndActivoTrue(Long empresaId);

    // Usado antes de editar/borrar: nunca se confía en un id que venga del
    // cliente sin validar que la regla sea de ESTA empresa, mismo criterio
    // que TiendaTestimonioRepository.findByIdAndEmpresaId.
    Optional<ReglaDescuentoCombo> findByIdAndEmpresaId(Long id, Long empresaId);
}
