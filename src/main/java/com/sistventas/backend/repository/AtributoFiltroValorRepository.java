package com.sistventas.backend.repository;

import com.sistventas.backend.entity.AtributoFiltroValor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AtributoFiltroValorRepository extends JpaRepository<AtributoFiltroValor, Long> {
    List<AtributoFiltroValor> findByAtributoFiltroIdOrderByOrdenAsc(Long atributoFiltroId);

    Optional<AtributoFiltroValor> findByIdAndAtributoFiltroId(Long id, Long atributoFiltroId);

    List<AtributoFiltroValor> findByAtributoFiltroIdIn(List<Long> atributoFiltroIds);
}
