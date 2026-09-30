package com.sistventas.backend.repository;

import com.sistventas.backend.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findByEmpresaIdAndActivoTrue(Long empresaId);

    List<Cliente> findByEmpresaIdAndActivoFalse(Long empresaId);

    boolean existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrue(Long empresaId, String nombre);

    boolean existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrueAndIdNot(Long empresaId, String nombre, Long id);

    Optional<Cliente> findByIdAndEmpresaId(Long id, Long empresaId);

    Optional<Cliente> findByEmpresaIdAndTelefono(Long empresaId, String telefono);

    long countByEmpresaIdAndActivoTrue(Long empresaId);

    // Login por email/contraseña (cuenta de cliente) — mismo criterio
    // (exact match) que UsuarioRepository.findByEmail para el panel admin.
    Optional<Cliente> findByEmpresaIdAndEmail(Long empresaId, String email);

    Optional<Cliente> findByEmpresaIdAndGoogleSub(Long empresaId, String googleSub);
}
