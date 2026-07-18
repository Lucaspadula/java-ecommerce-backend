package com.sistventas.backend.repository;

import com.sistventas.backend.entity.EstadoVenta;
import com.sistventas.backend.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    List<Venta> findByEmpresaIdAndEstado(Long empresaId, EstadoVenta estado);

    List<Venta> findByEmpresaIdAndEstadoNot(Long empresaId, EstadoVenta estado);

    List<Venta> findByEmpresaIdAndClienteIdOrderByFechaPedidoDesc(Long empresaId, Long clienteId);

    Optional<Venta> findByIdAndEmpresaId(Long id, Long empresaId);
}
