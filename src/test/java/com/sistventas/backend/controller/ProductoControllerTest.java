package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ActualizarProductoRequest;
import com.sistventas.backend.dto.AjustePrecioCategoriaRequest;
import com.sistventas.backend.dto.AjustePrecioCategoriaResultadoDto;
import com.sistventas.backend.dto.AjustarFotoRequest;
import com.sistventas.backend.dto.AsignarColorFotoRequest;
import com.sistventas.backend.dto.CrearResenaRequest;
import com.sistventas.backend.dto.FotoUploadDto;
import com.sistventas.backend.dto.ProductoDto;
import com.sistventas.backend.dto.ProductoRequest;
import com.sistventas.backend.dto.ReordenarFotosRequest;
import com.sistventas.backend.dto.ResenaDto;
import com.sistventas.backend.dto.TipoAjustePrecio;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.exception.ProductoNoEncontradoException;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.CatalogoService;
import com.sistventas.backend.service.ProductoService;
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
// criterio que ClienteControllerTest: interesa que ProductoController delegue
// en el service/catalogoService con los parámetros correctos y arme el
// ResponseEntity con el status HTTP esperado — no simular el dispatcher HTTP
// completo ni @AuthenticationPrincipal ni @Valid.
@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private ProductoService productoService;

    @Mock
    private CatalogoService catalogoService;

    @InjectMocks
    private ProductoController productoController;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void listarDevuelveOkConLaListaDelService() {
        ProductoDto dto = productoDto(1L, "Mate");
        when(productoService.listar(principal)).thenReturn(List.of(dto));

        ResponseEntity<List<ProductoDto>> respuesta = productoController.listar(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(productoService).listar(principal);
    }

    @Test
    void obtenerDevuelveOkConElProductoDelService() {
        ProductoDto dto = productoDto(5L, "Termo");
        when(productoService.obtener(5L, principal)).thenReturn(dto);

        ResponseEntity<ProductoDto> respuesta = productoController.obtener(5L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(productoService).obtener(5L, principal);
    }

    @Test
    void obtenerConIdInexistentePropagaLaExcepcionDelService() {
        when(productoService.obtener(404L, principal)).thenThrow(new ProductoNoEncontradoException());

        assertThatThrownBy(() -> productoController.obtener(404L, principal))
                .isInstanceOf(ProductoNoEncontradoException.class);
    }

    @Test
    void crearDevuelveCreatedConElProductoCreado() {
        ProductoRequest request = productoRequest("Mate");
        ProductoDto dto = productoDto(10L, "Mate");
        when(productoService.crear(request, principal)).thenReturn(dto);

        ResponseEntity<ProductoDto> respuesta = productoController.crear(request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(productoService).crear(request, principal);
    }

    @Test
    void actualizarDevuelveOkConElProductoActualizado() {
        ActualizarProductoRequest request = new ActualizarProductoRequest("Mate", 10L, null, null,
                new BigDecimal("100.00"), null, null, null, null, null, null, null, null, null);
        ProductoDto dto = productoDto(5L, "Mate");
        when(productoService.actualizar(5L, request, principal)).thenReturn(dto);

        ResponseEntity<ProductoDto> respuesta = productoController.actualizar(5L, request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(productoService).actualizar(5L, request, principal);
    }

    @Test
    void eliminarDevuelveNoContentYDelegaEnElService() {
        ResponseEntity<Void> respuesta = productoController.eliminar(5L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(respuesta.getBody()).isNull();
        verify(productoService).eliminar(5L, principal);
    }

    @Test
    void agregarFotoDevuelveOkConElProductoActualizadoYDelegaElArchivoYLaVariante() {
        MultipartFile file = mock(MultipartFile.class);
        ProductoDto dto = productoDto(5L, "Mate");
        when(productoService.agregarFoto(5L, file, 20L, principal)).thenReturn(dto);

        ResponseEntity<ProductoDto> respuesta = productoController.agregarFoto(5L, file, 20L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(productoService).agregarFoto(5L, file, 20L, principal);
    }

    @Test
    void agregarFotoSinVarianteIdDelegaConNull() {
        MultipartFile file = mock(MultipartFile.class);
        ProductoDto dto = productoDto(5L, "Mate");
        when(productoService.agregarFoto(5L, file, null, principal)).thenReturn(dto);

        ResponseEntity<ProductoDto> respuesta = productoController.agregarFoto(5L, file, null, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(productoService).agregarFoto(5L, file, null, principal);
    }

    @Test
    void eliminarFotoDevuelveOkConElProductoActualizadoYDelegaElFotoId() {
        ProductoDto dto = productoDto(5L, "Mate");
        when(productoService.eliminarFoto(5L, 3L, principal)).thenReturn(dto);

        ResponseEntity<ProductoDto> respuesta = productoController.eliminarFoto(5L, 3L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(productoService).eliminarFoto(5L, 3L, principal);
    }

    @Test
    void asignarColorFotoDevuelveOkYDelegaElVarianteIdDelBody() {
        ProductoDto dto = productoDto(5L, "Mate");
        AsignarColorFotoRequest request = new AsignarColorFotoRequest(20L);
        when(productoService.asignarColorFoto(5L, 3L, 20L, principal)).thenReturn(dto);

        ResponseEntity<ProductoDto> respuesta = productoController.asignarColorFoto(5L, 3L, request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(productoService).asignarColorFoto(5L, 3L, 20L, principal);
    }

    @Test
    void ajustarFotoDevuelveOkYDelegaElAgrandadaDelBody() {
        ProductoDto dto = productoDto(5L, "Mate");
        AjustarFotoRequest request = new AjustarFotoRequest(true);
        when(productoService.ajustarFoto(5L, 3L, true, principal)).thenReturn(dto);

        ResponseEntity<ProductoDto> respuesta = productoController.ajustarFoto(5L, 3L, request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(productoService).ajustarFoto(5L, 3L, true, principal);
    }

    @Test
    void reordenarFotosDevuelveOkYDelegaLaListaDeIdsDelBody() {
        ProductoDto dto = productoDto(5L, "Mate");
        ReordenarFotosRequest request = new ReordenarFotosRequest(List.of(3L, 1L, 2L));
        when(productoService.reordenarFotos(5L, List.of(3L, 1L, 2L), principal)).thenReturn(dto);

        ResponseEntity<ProductoDto> respuesta = productoController.reordenarFotos(5L, request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(productoService).reordenarFotos(5L, List.of(3L, 1L, 2L), principal);
    }

    @Test
    void subirFotoVarianteDevuelveOkConLaUrlDelService() {
        MultipartFile file = mock(MultipartFile.class);
        FotoUploadDto dto = new FotoUploadDto("/uploads/productos/variante.jpg");
        when(productoService.subirFotoVariante(file, principal)).thenReturn(dto);

        ResponseEntity<FotoUploadDto> respuesta = productoController.subirFotoVariante(file, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(productoService).subirFotoVariante(file, principal);
    }

    @Test
    void ajustarPrecioPorCategoriaDevuelveOkConElResultadoDelService() {
        AjustePrecioCategoriaRequest request =
                new AjustePrecioCategoriaRequest(10L, TipoAjustePrecio.PORCENTAJE, new BigDecimal("10"));
        AjustePrecioCategoriaResultadoDto resultado = new AjustePrecioCategoriaResultadoDto(3);
        when(productoService.ajustarPrecioPorCategoria(request, principal)).thenReturn(resultado);

        ResponseEntity<AjustePrecioCategoriaResultadoDto> respuesta =
                productoController.ajustarPrecioPorCategoria(request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(resultado);
        verify(productoService).ajustarPrecioPorCategoria(request, principal);
    }

    @Test
    void listarResenasDevuelveOkConLaListaDelService() {
        ResenaDto resena = new ResenaDto(1L, "Juana", "Excelente", 5, null, null, false);
        when(productoService.listarResenas(5L, principal)).thenReturn(List.of(resena));

        ResponseEntity<List<ResenaDto>> respuesta = productoController.listarResenas(5L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(resena);
        verify(productoService).listarResenas(5L, principal);
    }

    @Test
    void crearResenaDevuelveCreatedConLaResenaCreada() {
        CrearResenaRequest request = new CrearResenaRequest("Juana", "Excelente producto");
        ResenaDto resena = new ResenaDto(1L, "Juana", "Excelente producto", 5, null, null, false);
        when(productoService.crearResena(5L, request, principal)).thenReturn(resena);

        ResponseEntity<ResenaDto> respuesta = productoController.crearResena(5L, request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(resena);
        verify(productoService).crearResena(5L, request, principal);
    }

    @Test
    void eliminarResenaDevuelveNoContentYDelegaEnElService() {
        ResponseEntity<Void> respuesta = productoController.eliminarResena(5L, 1L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(respuesta.getBody()).isNull();
        verify(productoService).eliminarResena(5L, 1L, principal);
    }

    @Test
    void actualizarFotoResenaDevuelveOkConLaResenaActualizada() {
        MultipartFile file = mock(MultipartFile.class);
        ResenaDto resena = new ResenaDto(1L, "Juana", "Excelente", 5, null, "/uploads/resenas/foto.jpg", false);
        when(productoService.actualizarFotoResena(5L, 1L, file, principal)).thenReturn(resena);

        ResponseEntity<ResenaDto> respuesta = productoController.actualizarFotoResena(5L, 1L, file, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(resena);
        verify(productoService).actualizarFotoResena(5L, 1L, file, principal);
    }

    @Test
    void catalogoPdfDevuelveOkConContentTypePdfYElBytearrayDelService() {
        byte[] pdf = {1, 2, 3};
        when(catalogoService.generarPdf(principal)).thenReturn(pdf);

        ResponseEntity<byte[]> respuesta = productoController.catalogoPdf(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(pdf);
        assertThat(respuesta.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(respuesta.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"catalogo.pdf\"");
        verify(catalogoService).generarPdf(principal);
    }

    @Test
    void catalogoExcelDevuelveOkConContentTypeExcelYElBytearrayDelService() {
        byte[] excel = {4, 5, 6};
        when(catalogoService.generarExcel(principal)).thenReturn(excel);

        ResponseEntity<byte[]> respuesta = productoController.catalogoExcel(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(excel);
        assertThat(respuesta.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertThat(respuesta.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"catalogo.xlsx\"");
        verify(catalogoService).generarExcel(principal);
    }

    private ProductoDto productoDto(Long id, String nombre) {
        return new ProductoDto(id, nombre, "General", null, null, new BigDecimal("100.00"), null, null,
                null, List.of(), 0, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private ProductoRequest productoRequest(String nombre) {
        return new ProductoRequest(nombre, 10L, null, null, new BigDecimal("100.00"), null, null,
                null, null, null, null, null, null, null);
    }
}
