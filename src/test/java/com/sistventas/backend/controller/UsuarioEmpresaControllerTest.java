package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ActualizarActivoRequest;
import com.sistventas.backend.dto.InvitarUsuarioRequest;
import com.sistventas.backend.dto.UsuarioEmpresaDto;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.UsuarioEmpresaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests del controller invocando los métodos directamente (sin MockMvc): la
// restricción real a rolEmpresa=ADMIN vive en UsuarioEmpresaServiceImpl (ver
// AccesoRestringidoAdminException), ya cubierta en sus propios tests. Acá
// solo interesa que el controller delegue en el service con los parámetros
// correctos, arme el ResponseEntity esperado y no rompa esa cadena de
// validación cuando el service la dispara.
@ExtendWith(MockitoExtension.class)
class UsuarioEmpresaControllerTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private UsuarioEmpresaService usuarioEmpresaService;

    @InjectMocks
    private UsuarioEmpresaController usuarioEmpresaController;

    private final UserPrincipal principalAdmin = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);
    private final UserPrincipal principalMember = new UserPrincipal(100L, EMPRESA_ID, false, RolEmpresa.MEMBER);

    @Test
    void listarDevuelveOkConLaListaDelService() {
        UsuarioEmpresaDto dto = usuarioDto(1L, "Ana");
        when(usuarioEmpresaService.listar(principalAdmin)).thenReturn(List.of(dto));

        ResponseEntity<List<UsuarioEmpresaDto>> respuesta = usuarioEmpresaController.listar(principalAdmin);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(usuarioEmpresaService).listar(principalAdmin);
    }

    @Test
    void invitarDevuelveCreatedConElUsuarioInvitado() {
        InvitarUsuarioRequest request = new InvitarUsuarioRequest("Ana", "ana@test.com", "password123", RolEmpresa.MEMBER);
        UsuarioEmpresaDto dto = usuarioDto(5L, "Ana");
        when(usuarioEmpresaService.invitar(request, principalAdmin)).thenReturn(dto);

        ResponseEntity<UsuarioEmpresaDto> respuesta = usuarioEmpresaController.invitar(request, principalAdmin);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(usuarioEmpresaService).invitar(request, principalAdmin);
    }

    // El controller no valida rol: si un MEMBER llama a invitar, delega igual
    // en el service y es este quien rechaza la operación. Confirmamos que el
    // controller no atrapa ni oculta esa excepción.
    @Test
    void invitarConPrincipalMemberPropagaLaExcepcionDelService() {
        InvitarUsuarioRequest request = new InvitarUsuarioRequest("Ana", "ana@test.com", "password123", RolEmpresa.MEMBER);
        when(usuarioEmpresaService.invitar(request, principalMember))
                .thenThrow(new AccesoRestringidoAdminException());

        assertThatThrownBy(() -> usuarioEmpresaController.invitar(request, principalMember))
                .isInstanceOf(AccesoRestringidoAdminException.class);
    }

    @Test
    void actualizarActivoDevuelveOkConElUsuarioActualizado() {
        ActualizarActivoRequest request = new ActualizarActivoRequest(false);
        UsuarioEmpresaDto dto = usuarioDto(5L, "Ana");
        when(usuarioEmpresaService.actualizarActivo(5L, request, principalAdmin)).thenReturn(dto);

        ResponseEntity<UsuarioEmpresaDto> respuesta = usuarioEmpresaController.actualizarActivo(5L, request, principalAdmin);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(usuarioEmpresaService).actualizarActivo(5L, request, principalAdmin);
    }

    private UsuarioEmpresaDto usuarioDto(Long id, String nombre) {
        return new UsuarioEmpresaDto(id, nombre, nombre.toLowerCase() + "@test.com", RolEmpresa.MEMBER, true, LocalDateTime.now());
    }
}
