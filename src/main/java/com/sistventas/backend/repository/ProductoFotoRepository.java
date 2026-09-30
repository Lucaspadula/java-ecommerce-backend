package com.sistventas.backend.repository;

import com.sistventas.backend.entity.ProductoFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoFotoRepository extends JpaRepository<ProductoFoto, Long> {
    List<ProductoFoto> findByProductoIdOrderByOrdenAsc(Long productoId);

    long countByProductoId(Long productoId);

    // Mismo criterio multi-tenant que el resto de los repositorios de
    // sub-recursos de Producto (ej. ResenaRepository): valida que la foto
    // pertenezca al producto puntual (que a su vez ya se validó por empresa
    // más arriba en el service), nunca solo por su id suelto.
    Optional<ProductoFoto> findByIdAndProductoId(Long id, Long productoId);
}
