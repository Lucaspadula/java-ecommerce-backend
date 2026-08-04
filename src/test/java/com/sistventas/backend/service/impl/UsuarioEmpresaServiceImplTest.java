package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarActivoRequest;
import com.sistventas.backend.dto.InvitarUsuarioRequest;
import com.sistventas.backend.dto.UsuarioEmpresaDto;
import com.sistventas.backend.entity.Empresa;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests de UsuarioEmpresaServiceImpl: gestión de usuarios dentro de la
// empresa del ADMIN logueado. Foco en las reglas de negocio reales que tiene
// el service (no se inventan reglas que no existan en el código):
// - Solo ADMIN puede listar/invitar/activar-desactivar (adminEmpresaIdOrThrow).
// - El scoping siempre es por empresaId del principal, nunca de otra empresa.
// - Email duplicado no se puede invitar.
// - Un ADMIN no puede desactivar su PROPIO usuario (no hay, en este service,
//   ninguna protección contra desactivar/degradar al ÚLTIMO admin de la
//   empresa vía otro id, ni un endpoint para cambiar rolEmpresa — ver nota
//   al final del archivo).
@ExtendWith(MockitoExtension.class)
class UsuarioEmpresaServiceImplTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioEmpresaServiceImpl service;

    private final UserPrincipal admin = new UserPrincipal(1L, EMPRESA_ID, false, RolEmpresa.ADMIN);
    private final UserPrincipal member = new UserPrincipal(2L, EMPRESA_ID, false, RolEmpresa.MEMBER);
    private final UserPrincipal sinEmpresa = new UserPrincipal(3L, null, false, null);

    // ---------- listar ----------

    @Test
    void listarConAdminDevuelveSoloLosUsuariosDeSuEmpresa() {
        Usuario usuario = usuario(10L, "Ana", "ana@test.com", RolEmpresa.MEMBER, true);
        when(usuarioRepository.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of(usuario));

        List<UsuarioEmpresaDto> resultado = service.listar(admin);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).email()).isEqualTo("ana@test.com");
        verify(usuarioRepository).findByEmpresaId(EMPRESA_ID);
    }

    @Test
    void listarConRolMemberLanzaAccesoRestringidoAdminException() {
        assertThatThrownBy(() -> service.listar(member))
                .isInstanceOf(AccesoRestringidoAdminException.class);

        verify(usuarioRepository, never()).findByEmpresaId(any());
    }

    @Test
    void listarSinEmpresaLanzaSinEmpresaException() {
        // Caso del Super Admin: autenticado pero sin empresaId.
        assertThatThrownBy(() -> service.listar(sinEmpresa))
                .isInstanceOf(SinEmpresaException.class);
    }

    // ---------- invitar ----------

    @Test
    void invitarConEmailLibreCreaUsuarioActivoNoSuperAdminDeLaEmpresaDelPrincipal() {
        InvitarUsuarioRequest request = new InvitarUsuarioRequest("Pedro", "pedro@test.com", "password123", RolEmpresa.MEMBER);
        when(usuarioRepository.findByEmail("pedro@test.com")).thenReturn(Optional.empty());
        when(empresaRepository.getReferenceById(EMPRESA_ID)).thenReturn(empresa(EMPRESA_ID));
        when(passwordEncoder.encode("password123")).thenReturn("hash-encriptado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioEmpresaDto resultado = service.invitar(request, admin);

        assertThat(resultado.nombre()).isEqualTo("Pedro");
        assertThat(resultado.rolEmpresa()).isEqualTo(RolEmpresa.MEMBER);
        assertThat(resultado.activo()).isTrue();
        verify(passwordEncoder).encode("password123");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void invitarConEmailYaRegistradoLanzaEmailYaRegistradoException() {
        InvitarUsuarioRequest request = new InvitarUsuarioRequest("Pedro", "pedro@test.com", "password123", RolEmpresa.MEMBER);
        when(usuarioRepository.findByEmail("pedro@test.com"))
                .thenReturn(Optional.of(usuario(20L, "Otro", "pedro@test.com", RolEmpresa.MEMBER, true)));

        assertThatThrownBy(() -> service.invitar(request, admin))
                .isInstanceOf(EmailYaRegistradoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void invitarConRolMemberLanzaAccesoRestringidoAdminException() {
        InvitarUsuarioRequest request = new InvitarUsuarioRequest("Pedro", "pedro@test.com", "password123", RolEmpresa.MEMBER);

        assertThatThrownBy(() -> service.invitar(request, member))
                .isInstanceOf(AccesoRestringidoAdminException.class);

        verify(usuarioRepository, never()).findByEmail(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void invitarSinEmpresaLanzaSinEmpresaException() {
        InvitarUsuarioRequest request = new InvitarUsuarioRequest("Pedro", "pedro@test.com", "password123", RolEmpresa.MEMBER);

        assertThatThrownBy(() -> service.invitar(request, sinEmpresa))
                .isInstanceOf(SinEmpresaException.class);
    }

    // ---------- actualizarActivo ----------

    @Test
    void actualizarActivoDesactivaUnUsuarioDeLaEmpresaQueNoEsElPrincipal() {
        Usuario usuario = usuario(10L, "Ana", "ana@test.com", RolEmpresa.MEMBER, true);
        when(usuarioRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioEmpresaDto resultado = service.actualizarActivo(10L, new ActualizarActivoRequest(false), admin);

        assertThat(resultado.activo()).isFalse();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void actualizarActivoSobreElPropioUsuarioDelPrincipalLanzaAccionNoPermitidaException() {
        // Regla real del service: nadie puede desactivar su propio usuario,
        // sin importar si es el único ADMIN o no (no hay chequeo de "último
        // admin", el chequeo es puramente "es el mismo id que el principal").
        assertThatThrownBy(() -> service.actualizarActivo(admin.id(), new ActualizarActivoRequest(false), admin))
                .isInstanceOf(AccionNoPermitidaException.class);

        verify(usuarioRepository, never()).findByIdAndEmpresaId(any(), any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizarActivoSobreUsuarioDeOtraEmpresaLanzaUsuarioNoEncontradoException() {
        // El id existe en la base pero pertenece a OTRA empresa: el
        // findByIdAndEmpresaId scopeado no lo encuentra, nunca se filtra
        // solo por id.
        when(usuarioRepository.findByIdAndEmpresaId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarActivo(99L, new ActualizarActivoRequest(false), admin))
                .isInstanceOf(UsuarioNoEncontradoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizarActivoConRolMemberLanzaAccesoRestringidoAdminException() {
        assertThatThrownBy(() -> service.actualizarActivo(10L, new ActualizarActivoRequest(false), member))
                .isInstanceOf(AccesoRestringidoAdminException.class);

        verify(usuarioRepository, never()).findByIdAndEmpresaId(any(), any());
    }

    @Test
    void actualizarActivoReactivaUnUsuarioInactivo() {
        Usuario usuario = usuario(10L, "Ana", "ana@test.com", RolEmpresa.MEMBER, false);
        when(usuarioRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioEmpresaDto resultado = service.actualizarActivo(10L, new ActualizarActivoRequest(true), admin);

        assertThat(resultado.activo()).isTrue();
    }

    // ---------- helpers ----------

    private Usuario usuario(Long id, String nombre, String email, RolEmpresa rol, boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setRolEmpresa(rol);
        usuario.setActivo(activo);
        usuario.setEsSuperAdmin(false);
        return usuario;
    }

    private Empresa empresa(Long id) {
        Empresa empresa = new Empresa();
        empresa.setId(id);
        return empresa;
    }
}
