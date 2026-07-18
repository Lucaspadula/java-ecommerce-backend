package com.sistventas.backend.repository;

import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByGoogleSub(String googleSub);

    boolean existsByEsSuperAdminTrue();

    List<Usuario> findByEmpresaIdAndRolEmpresa(Long empresaId, RolEmpresa rolEmpresa);

    List<Usuario> findByEmpresaId(Long empresaId);

    Optional<Usuario> findByIdAndEmpresaId(Long id, Long empresaId);
}
