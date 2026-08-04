package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ImportarInsumosResultadoDto;
import com.sistventas.backend.dto.InsumoDto;
import com.sistventas.backend.dto.InsumoRequest;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.exception.InsumoNoEncontradoException;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.InsumoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
// criterio que ClienteControllerTest/ProductoControllerTest: interesa que
// InsumoController delegue en el service con los parámetros correctos y arme
// el ResponseEntity con el status HTTP esperado.
@ExtendWith(MockitoExtension.class)
class InsumoControllerTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private InsumoService insumoService;

    @InjectMocks
    private InsumoController insumoController;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void listarDevuelveOkConLaListaDelService() {
        InsumoDto dto = insumoDto(1L, "Madera");
        when(insumoService.listar(principal)).thenReturn(List.of(dto));

        ResponseEntity<List<InsumoDto>> respuesta = insumoController.listar(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(insumoService).listar(principal);
    }

    @Test
    void listarInactivosDevuelveOkConLaListaDelService() {
        InsumoDto dto = insumoDto(2L, "Tela");
        when(insumoService.listarInactivos(principal)).thenReturn(List.of(dto));

        ResponseEntity<List<InsumoDto>> respuesta = insumoController.listarInactivos(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(insumoService).listarInactivos(principal);
    }

    @Test
    void obtenerDevuelveOkConElInsumoDelService() {
        InsumoDto dto = insumoDto(5L, "Hilo");
        when(insumoService.obtener(5L, principal)).thenReturn(dto);

        ResponseEntity<InsumoDto> respuesta = insumoController.obtener(5L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(insumoService).obtener(5L, principal);
    }

    @Test
    void obtenerConIdInexistentePropagaLaExcepcionDelService() {
        when(insumoService.obtener(404L, principal)).thenThrow(new InsumoNoEncontradoException());

        assertThatThrownBy(() -> insumoController.obtener(404L, principal))
                .isInstanceOf(InsumoNoEncontradoException.class);
    }

    @Test
    void crearDevuelveCreatedConElInsumoCreado() {
        InsumoRequest request = insumoRequest("Madera");
        InsumoDto dto = insumoDto(10L, "Madera");
        when(insumoService.crear(request, principal)).thenReturn(dto);

        ResponseEntity<InsumoDto> respuesta = insumoController.crear(request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(insumoService).crear(request, principal);
    }

    @Test
    void actualizarDevuelveOkConElInsumoActualizado() {
        InsumoRequest request = insumoRequest("Madera");
        InsumoDto dto = insumoDto(5L, "Madera");
        when(insumoService.actualizar(5L, request, principal)).thenReturn(dto);

        ResponseEntity<InsumoDto> respuesta = insumoController.actualizar(5L, request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(insumoService).actualizar(5L, request, principal);
    }

    @Test
    void eliminarDevuelveNoContentYDelegaEnElService() {
        ResponseEntity<Void> respuesta = insumoController.eliminar(5L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(respuesta.getBody()).isNull();
        verify(insumoService).eliminar(5L, principal);
    }

    @Test
    void restaurarDevuelveOkConElInsumoRestaurado() {
        InsumoDto dto = insumoDto(7L, "Hilo");
        when(insumoService.restaurar(7L, principal)).thenReturn(dto);

        ResponseEntity<InsumoDto> respuesta = insumoController.restaurar(7L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(insumoService).restaurar(7L, principal);
    }

    @Test
    void plantillaDevuelveOkConContentTypeExcelYElBytearrayDelService() {
        byte[] excel = {1, 2, 3};
        when(insumoService.generarPlantilla()).thenReturn(excel);

        ResponseEntity<byte[]> respuesta = insumoController.plantilla();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(excel);
        assertThat(respuesta.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertThat(respuesta.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"plantilla-insumos.xlsx\"");
        verify(insumoService).generarPlantilla();
    }

    @Test
    void importarDevuelveOkConElResultadoDelService() {
        MultipartFile file = mock(MultipartFile.class);
        ImportarInsumosResultadoDto resultado = new ImportarInsumosResultadoDto(3, List.of());
        when(insumoService.importarDesdeExcel(file, principal)).thenReturn(resultado);

        ResponseEntity<ImportarInsumosResultadoDto> respuesta = insumoController.importar(file, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(resultado);
        verify(insumoService).importarDesdeExcel(file, principal);
    }

    @Test
    void importarConFilasInvalidasDevuelveOkConLosErroresDelService() {
        MultipartFile file = mock(MultipartFile.class);
        ImportarInsumosResultadoDto resultado =
                new ImportarInsumosResultadoDto(1, List.of("Fila 3: nombre obligatorio"));
        when(insumoService.importarDesdeExcel(file, principal)).thenReturn(resultado);

        ResponseEntity<ImportarInsumosResultadoDto> respuesta = insumoController.importar(file, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().creados()).isEqualTo(1);
        assertThat(respuesta.getBody().errores()).containsExactly("Fila 3: nombre obligatorio");
    }

    private InsumoDto insumoDto(Long id, String nombre) {
        return new InsumoDto(id, nombre, new BigDecimal("5.00"), new BigDecimal("100"), null, "unidad", true,
                null, null);
    }

    private InsumoRequest insumoRequest(String nombre) {
        return new InsumoRequest(nombre, new BigDecimal("5.00"), new BigDecimal("100"), null, "unidad", null);
    }
}
