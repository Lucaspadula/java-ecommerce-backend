package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ActualizarEstadoVentaRequest;
import com.sistventas.backend.dto.FotoUploadDto;
import com.sistventas.backend.dto.TextoCompartirDto;
import com.sistventas.backend.dto.VentaDto;
import com.sistventas.backend.dto.VentaEstadoHistorialDto;
import com.sistventas.backend.dto.VentaItemRequest;
import com.sistventas.backend.dto.VentaRequest;
import com.sistventas.backend.entity.EstadoVenta;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.exception.VentaNoEncontradaException;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.VentaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests del controller invocando los métodos directamente (sin MockMvc), mismo
// criterio que ClienteControllerTest/ProductoControllerTest/InsumoControllerTest:
// interesa que VentaController delegue en el service con los parámetros
// correctos y arme el ResponseEntity con el status HTTP esperado.
@ExtendWith(MockitoExtension.class)
class VentaControllerTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private VentaService ventaService;

    @InjectMocks
    private VentaController ventaController;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void listarSinFiltrosDevuelveOkConLaListaDelService() {
        VentaDto dto = ventaDto(1L, EstadoVenta.PRESUPUESTO);
        when(ventaService.listar(null, null, principal)).thenReturn(List.of(dto));

        ResponseEntity<List<VentaDto>> respuesta = ventaController.listar(null, null, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(ventaService).listar(null, null, principal);
    }

    @Test
    void listarConFiltrosDeEstadoYClienteLosPropagaAlService() {
        VentaDto dto = ventaDto(2L, EstadoVenta.CONFIRMADA);
        when(ventaService.listar(EstadoVenta.CONFIRMADA, 7L, principal)).thenReturn(List.of(dto));

        ResponseEntity<List<VentaDto>> respuesta =
                ventaController.listar(EstadoVenta.CONFIRMADA, 7L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(ventaService).listar(EstadoVenta.CONFIRMADA, 7L, principal);
    }

    @Test
    void obtenerDevuelveOkConLaVentaDelService() {
        VentaDto dto = ventaDto(5L, EstadoVenta.PRESUPUESTO);
        when(ventaService.obtener(5L, principal)).thenReturn(dto);

        ResponseEntity<VentaDto> respuesta = ventaController.obtener(5L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(ventaService).obtener(5L, principal);
    }

    @Test
    void obtenerConIdInexistentePropagaLaExcepcionDelService() {
        when(ventaService.obtener(404L, principal)).thenThrow(new VentaNoEncontradaException());

        assertThatThrownBy(() -> ventaController.obtener(404L, principal))
                .isInstanceOf(VentaNoEncontradaException.class);
    }

    @Test
    void crearDevuelveCreatedConLaVentaCreada() {
        VentaRequest request = ventaRequest();
        VentaDto dto = ventaDto(10L, EstadoVenta.PRESUPUESTO);
        when(ventaService.crear(request, principal)).thenReturn(dto);

        ResponseEntity<VentaDto> respuesta = ventaController.crear(request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(ventaService).crear(request, principal);
    }

    @Test
    void actualizarDevuelveOkConLaVentaActualizada() {
        VentaRequest request = ventaRequest();
        VentaDto dto = ventaDto(5L, EstadoVenta.PRESUPUESTO);
        when(ventaService.actualizar(5L, request, principal)).thenReturn(dto);

        ResponseEntity<VentaDto> respuesta = ventaController.actualizar(5L, request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(ventaService).actualizar(5L, request, principal);
    }

    @Test
    void actualizarEstadoDevuelveOkConLaVentaActualizadaYDelegaElNuevoEstado() {
        ActualizarEstadoVentaRequest request = new ActualizarEstadoVentaRequest(EstadoVenta.CONFIRMADA);
        VentaDto dto = ventaDto(5L, EstadoVenta.CONFIRMADA);
        when(ventaService.actualizarEstado(5L, request, principal)).thenReturn(dto);

        ResponseEntity<VentaDto> respuesta = ventaController.actualizarEstado(5L, request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(ventaService).actualizarEstado(5L, request, principal);
    }

    @Test
    void eliminarDevuelveNoContentYDelegaEnElService() {
        ResponseEntity<Void> respuesta = ventaController.eliminar(5L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(respuesta.getBody()).isNull();
        verify(ventaService).eliminar(5L, principal);
    }

    @Test
    void historialEstadosDevuelveOkConLaListaDelService() {
        VentaEstadoHistorialDto historial =
                new VentaEstadoHistorialDto(1L, EstadoVenta.PRESUPUESTO, EstadoVenta.CONFIRMADA, "Juan", null);
        when(ventaService.historialEstados(5L, principal)).thenReturn(List.of(historial));

        ResponseEntity<List<VentaEstadoHistorialDto>> respuesta = ventaController.historialEstados(5L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(historial);
        verify(ventaService).historialEstados(5L, principal);
    }

    @Test
    void textoCompartirDevuelveOkConElTextoDelService() {
        TextoCompartirDto texto = new TextoCompartirDto("Pedido #5 confirmado", "1155555555");
        when(ventaService.textoCompartir(5L, principal)).thenReturn(texto);

        ResponseEntity<TextoCompartirDto> respuesta = ventaController.textoCompartir(5L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(texto);
        verify(ventaService).textoCompartir(5L, principal);
    }

    @Test
    void subirFotoDevuelveOkConLaUrlDelService() {
        MultipartFile file = mock(MultipartFile.class);
        FotoUploadDto dto = new FotoUploadDto("/uploads/ventas/foto.jpg");
        when(ventaService.subirFoto(file, principal)).thenReturn(dto);

        ResponseEntity<FotoUploadDto> respuesta = ventaController.subirFoto(file, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(ventaService).subirFoto(file, principal);
    }

    private VentaDto ventaDto(Long id, EstadoVenta estado) {
        return new VentaDto(id, 1L, "Juan", estado, null, null, null,
                new BigDecimal("100.00"), new BigDecimal("100.00"), null, List.of(), null,
                null, null, null, null, null);
    }

    private VentaRequest ventaRequest() {
        return new VentaRequest(1L, null, null, null,
                List.of(new VentaItemRequest(1L, 2, null, null, null, null)));
    }
}
