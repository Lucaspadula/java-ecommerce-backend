package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ProveedorDto;
import com.sistventas.backend.dto.ProveedorRequest;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.exception.ProveedorNoEncontradoException;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.ProveedorService;
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
// que interesa acá es que ProveedorController delegue en el service con los
// parámetros correctos y arme el ResponseEntity con el status HTTP esperado,
// no simular el dispatcher HTTP completo ni @AuthenticationPrincipal.
@ExtendWith(MockitoExtension.class)
class ProveedorControllerTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private ProveedorService proveedorService;

    @InjectMocks
    private ProveedorController proveedorController;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void listarDevuelveOkConLaListaDelService() {
        ProveedorDto dto = proveedorDto(1L, "Distribuidora SA");
        when(proveedorService.listar(principal)).thenReturn(List.of(dto));

        ResponseEntity<List<ProveedorDto>> respuesta = proveedorController.listar(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(proveedorService).listar(principal);
    }

    @Test
    void listarInactivosDevuelveOkConLaListaDelService() {
        ProveedorDto dto = proveedorDto(2L, "Insumos del Sur");
        when(proveedorService.listarInactivos(principal)).thenReturn(List.of(dto));

        ResponseEntity<List<ProveedorDto>> respuesta = proveedorController.listarInactivos(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(proveedorService).listarInactivos(principal);
    }

    @Test
    void obtenerDevuelveOkConElProveedorDelService() {
        ProveedorDto dto = proveedorDto(5L, "Mayorista Central");
        when(proveedorService.obtener(5L, principal)).thenReturn(dto);

        ResponseEntity<ProveedorDto> respuesta = proveedorController.obtener(5L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(proveedorService).obtener(5L, principal);
    }

    @Test
    void obtenerConIdInexistentePropagaLaExcepcionDelService() {
        when(proveedorService.obtener(404L, principal)).thenThrow(new ProveedorNoEncontradoException());

        assertThatThrownBy(() -> proveedorController.obtener(404L, principal))
                .isInstanceOf(ProveedorNoEncontradoException.class);
    }

    @Test
    void crearDevuelveCreatedConElProveedorCreado() {
        ProveedorRequest request = new ProveedorRequest("Mayorista Central", null, null, null, null, null, null, null);
        ProveedorDto dto = proveedorDto(10L, "Mayorista Central");
        when(proveedorService.crear(request, principal)).thenReturn(dto);

        ResponseEntity<ProveedorDto> respuesta = proveedorController.crear(request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(proveedorService).crear(request, principal);
    }

    @Test
    void actualizarDevuelveOkConElProveedorActualizado() {
        ProveedorRequest request = new ProveedorRequest("Insumos del Sur", null, null, null, null, null, null, null);
        ProveedorDto dto = proveedorDto(5L, "Insumos del Sur");
        when(proveedorService.actualizar(5L, request, principal)).thenReturn(dto);

        ResponseEntity<ProveedorDto> respuesta = proveedorController.actualizar(5L, request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(proveedorService).actualizar(5L, request, principal);
    }

    @Test
    void eliminarDevuelveNoContentYDelegaEnElService() {
        ResponseEntity<Void> respuesta = proveedorController.eliminar(5L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(respuesta.getBody()).isNull();
        verify(proveedorService).eliminar(5L, principal);
    }

    @Test
    void restaurarDevuelveOkConElProveedorRestaurado() {
        ProveedorDto dto = proveedorDto(7L, "Distribuidora SA");
        when(proveedorService.restaurar(7L, principal)).thenReturn(dto);

        ResponseEntity<ProveedorDto> respuesta = proveedorController.restaurar(7L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(proveedorService).restaurar(7L, principal);
    }

    private ProveedorDto proveedorDto(Long id, String nombre) {
        return new ProveedorDto(id, nombre, null, null, null, null, null, null, null, LocalDateTime.now());
    }
}
