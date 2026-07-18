package com.sistventas.backend.repository;

import com.sistventas.backend.entity.VentaEstadoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VentaEstadoHistorialRepository extends JpaRepository<VentaEstadoHistorial, Long> {
    // Desempata por id cuando dos cambios caen en el mismo segundo (DATETIME
    // sin milisegundos en MariaDB) — sin esto, el orden entre ellos queda
    // indefinido y puede mostrar el más nuevo antes que el más viejo.
    List<VentaEstadoHistorial> findByVentaIdOrderByFechaDescIdDesc(Long ventaId);
}
