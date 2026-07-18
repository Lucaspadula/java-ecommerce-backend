package com.sistventas.backend.repository;

import com.sistventas.backend.entity.Empresa;
import com.sistventas.backend.entity.LicenciaEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    List<Empresa> findByLicenciaEstado(LicenciaEstado licenciaEstado);

    Optional<Empresa> findBySlug(String slug);

    Optional<Empresa> findBySlugAndTiendaHabilitadaTrue(String slug);
}
