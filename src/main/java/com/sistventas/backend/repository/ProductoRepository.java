package com.sistventas.backend.repository;

import com.sistventas.backend.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByEmpresaIdAndActivoTrue(Long empresaId);

    Optional<Producto> findByIdAndEmpresaId(Long id, Long empresaId);

    long countByEmpresaIdAndActivoTrue(Long empresaId);

    List<Producto> findByEmpresaIdAndCategoriaIdAndActivoTrue(Long empresaId, Long categoriaId);
}
