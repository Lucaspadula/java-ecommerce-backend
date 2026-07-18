package com.sistventas.backend.repository;

import com.sistventas.backend.entity.Resena;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResenaRepository extends JpaRepository<Resena, Long> {
    List<Resena> findByProductoIdOrderByFechaDesc(Long productoId);

    // Todas las reseñas de la empresa, ordenadas por fecha desc: usado para
    // resolver la reseña MÁS RECIENTE de CADA producto del catálogo público
    // en un solo query (ver PublicTiendaServiceImpl.listarProductos), en vez
    // de un N+1 de findByProductoIdOrderByFechaDesc por producto.
    List<Resena> findByEmpresaIdOrderByFechaDesc(Long empresaId);

    // Usado antes de borrar: valida que la reseña (por su propio id)
    // pertenezca a ESE producto Y a la empresa del usuario, no solo el id de
    // la reseña suelto — mismo criterio que ProductoRepository.findByIdAndEmpresaId.
    Optional<Resena> findByIdAndProductoIdAndEmpresaId(Long id, Long productoId, Long empresaId);
}
