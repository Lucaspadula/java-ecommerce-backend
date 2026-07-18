package com.sistventas.backend.repository;

import com.sistventas.backend.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findByEmpresaIdAndActivoTrue(Long empresaId);

    Optional<Cliente> findByIdAndEmpresaId(Long id, Long empresaId);

    Optional<Cliente> findByEmpresaIdAndTelefono(Long empresaId, String telefono);

    long countByEmpresaIdAndActivoTrue(Long empresaId);
}
