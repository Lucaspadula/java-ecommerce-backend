package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarActivoRequest;
import com.sistventas.backend.dto.InvitarUsuarioRequest;
import com.sistventas.backend.dto.UsuarioEmpresaDto;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.Usuario;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.exception.AccionNoPermitidaException;
import com.sistventas.backend.exception.EmailYaRegistradoException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.exception.UsuarioNoEncontradoException;
import com.sistventas.backend.repository.EmpresaRepository;
import com.sistventas.backend.repository.UsuarioRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.UsuarioEmpresaService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UsuarioEmpresaServiceImpl implements UsuarioEmpresaService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioEmpresaServiceImpl(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioEmpresaDto> listar(UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        return usuarioRepository.findByEmpresaId(empresaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public UsuarioEmpresaDto invitar(InvitarUsuarioRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);

        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailYaRegistradoException();
        }

        Usuario usuario = new Usuario();
        usuario.setEmpresa(empresaRepository.getReferenceById(empresaId));
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRolEmpresa(request.rolEmpresa());
        usuario.setEsSuperAdmin(false);
        usuario.setActivo(true);
        usuario.setFechaAlta(LocalDateTime.now());

        return toDto(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioEmpresaDto actualizarActivo(Long id, ActualizarActivoRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);

        if (id.equals(principal.id())) {
            throw new AccionNoPermitidaException("No podés desactivar tu propio usuario");
        }

        Usuario usuario = usuarioRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(UsuarioNoEncontradoException::new);
        usuario.setActivo(request.activo());

        return toDto(usuarioRepository.save(usuario));
    }

    // Análogo a empresaIdOrThrow (ver ProductoServiceImpl), pero además exige
    // rol ADMIN: la gestión de usuarios de la empresa es exclusiva del admin,
    // nunca de un MEMBER ni del Super Admin (que no tiene empresa).
    private Long adminEmpresaIdOrThrow(UserPrincipal principal) {
        if (principal == null || principal.empresaId() == null) {
            throw new SinEmpresaException();
        }
        if (principal.rolEmpresa() != RolEmpresa.ADMIN) {
            throw new AccesoRestringidoAdminException();
        }
        return principal.empresaId();
    }

    private UsuarioEmpresaDto toDto(Usuario usuario) {
        return new UsuarioEmpresaDto(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRolEmpresa(),
                usuario.isActivo(),
                usuario.getFechaAlta()
        );
    }
}
