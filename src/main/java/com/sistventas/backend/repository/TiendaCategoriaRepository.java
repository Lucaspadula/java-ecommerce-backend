package com.sistventas.backend.repository;

import com.sistventas.backend.entity.TiendaCategoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TiendaCategoriaRepository extends JpaRepository<TiendaCategoria, Long> {
    List<TiendaCategoria> findByEmpresaId(Long empresaId);

    Optional<TiendaCategoria> findByEmpresaIdAndCategoriaId(Long empresaId, Long categoriaId);
}
