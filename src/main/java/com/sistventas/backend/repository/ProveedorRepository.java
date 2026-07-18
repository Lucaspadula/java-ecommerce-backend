package com.sistventas.backend.repository;

import com.sistventas.backend.entity.EstadoPedido;
import com.sistventas.backend.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    List<Proveedor> findByEmpresaIdAndActivoTrue(Long empresaId);

    Optional<Proveedor> findByIdAndEmpresaId(Long id, Long empresaId);

    long countByEmpresaIdAndEstadoPedido(Long empresaId, EstadoPedido estadoPedido);
}
