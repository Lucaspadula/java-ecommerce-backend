package com.sistventas.backend.repository;

import com.sistventas.backend.entity.ProductoStockAjuste;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoStockAjusteRepository extends JpaRepository<ProductoStockAjuste, Long> {
    List<ProductoStockAjuste> findByProductoIdOrderByFechaDesc(Long productoId);
}
