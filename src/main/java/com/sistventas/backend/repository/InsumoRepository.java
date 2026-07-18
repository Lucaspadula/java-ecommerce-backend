package com.sistventas.backend.repository;

import com.sistventas.backend.entity.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {
    List<Insumo> findByEmpresaIdAndActivoTrue(Long empresaId);

    Optional<Insumo> findByIdAndEmpresaId(Long id, Long empresaId);
}
