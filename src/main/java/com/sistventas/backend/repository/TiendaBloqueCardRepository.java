package com.sistventas.backend.repository;

import com.sistventas.backend.entity.TiendaBloqueCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TiendaBloqueCardRepository extends JpaRepository<TiendaBloqueCard, Long> {

    List<TiendaBloqueCard> findByBloqueIdOrderByOrdenAscIdAsc(Long bloqueId);

    // Una sola query para las cards de varios bloques (evita el N+1).
    List<TiendaBloqueCard> findByBloqueIdInOrderByOrdenAscIdAsc(Collection<Long> bloqueIds);

    long countByBloqueId(Long bloqueId);

    Optional<TiendaBloqueCard> findByIdAndBloqueId(Long id, Long bloqueId);

    void deleteByBloqueId(Long bloqueId);
}
