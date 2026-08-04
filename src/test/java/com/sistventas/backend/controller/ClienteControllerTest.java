package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ClienteDto;
import com.sistventas.backend.dto.ClienteRequest;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.exception.ClienteNoEncontradoException;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.ClienteService;
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

// Tests del controller invocando los métodos directamente (sin MockMvc): lo
// que interesa acá es que ClienteController delegue en el service con los
// parámetros correctos y arme el ResponseEntity con el status HTTP esperado,
// no simular el dispatcher HTTP completo ni @AuthenticationPrincipal.
@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void listarDevuelveOkConLaListaDelService() {
        ClienteDto dto = clienteDto(1L, "Juan");
        when(clienteService.listar(principal)).thenReturn(List.of(dto));

        ResponseEntity<List<ClienteDto>> respuesta = clienteController.listar(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(clienteService).listar(principal);
    }

    @Test
    void listarInactivosDevuelveOkConLaListaDelService() {
        ClienteDto dto = clienteDto(2L, "Ana");
        when(clienteService.listarInactivos(principal)).thenReturn(List.of(dto));

        ResponseEntity<List<ClienteDto>> respuesta = clienteController.listarInactivos(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(clienteService).listarInactivos(principal);
    }

    @Test
    void obtenerDevuelveOkConElClienteDelService() {
        ClienteDto dto = clienteDto(5L, "Pedro");
        when(clienteService.obtener(5L, principal)).thenReturn(dto);

        ResponseEntity<ClienteDto> respuesta = clienteController.obtener(5L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(clienteService).obtener(5L, principal);
    }

    @Test
    void obtenerConIdInexistentePropagaLaExcepcionDelService() {
        when(clienteService.obtener(404L, principal)).thenThrow(new ClienteNoEncontradoException());

        assertThatThrownBy(() -> clienteController.obtener(404L, principal))
                .isInstanceOf(ClienteNoEncontradoException.class);
    }

    @Test
    void crearDevuelveCreatedConElClienteCreado() {
        ClienteRequest request = new ClienteRequest("Pedro", null, null, null);
        ClienteDto dto = clienteDto(10L, "Pedro");
        when(clienteService.crear(request, principal)).thenReturn(dto);

        ResponseEntity<ClienteDto> respuesta = clienteController.crear(request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(clienteService).crear(request, principal);
    }

    @Test
    void actualizarDevuelveOkConElClienteActualizado() {
        ClienteRequest request = new ClienteRequest("Ana", null, null, null);
        ClienteDto dto = clienteDto(5L, "Ana");
        when(clienteService.actualizar(5L, request, principal)).thenReturn(dto);

        ResponseEntity<ClienteDto> respuesta = clienteController.actualizar(5L, request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(clienteService).actualizar(5L, request, principal);
    }

    @Test
    void eliminarDevuelveNoContentYDelegaEnElService() {
        ResponseEntity<Void> respuesta = clienteController.eliminar(5L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(respuesta.getBody()).isNull();
        verify(clienteService).eliminar(5L, principal);
    }

    @Test
    void restaurarDevuelveOkConElClienteRestaurado() {
        ClienteDto dto = clienteDto(7L, "Luis");
        when(clienteService.restaurar(7L, principal)).thenReturn(dto);

        ResponseEntity<ClienteDto> respuesta = clienteController.restaurar(7L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(clienteService).restaurar(7L, principal);
    }

    private ClienteDto clienteDto(Long id, String nombre) {
        return new ClienteDto(id, nombre, null, null, null, LocalDateTime.now());
    }
}
