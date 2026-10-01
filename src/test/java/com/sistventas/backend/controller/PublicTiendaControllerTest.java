package com.sistventas.backend.controller;

import com.sistventas.backend.dto.AtributoFiltroDto;
import com.sistventas.backend.dto.AtributoFiltroValorDto;
import com.sistventas.backend.dto.CategoriaTiendaDto;
import com.sistventas.backend.dto.PreviewDescuentoComboDto;
import com.sistventas.backend.dto.PreviewDescuentoComboRequest;
import com.sistventas.backend.dto.PublicEmpresaDto;
import com.sistventas.backend.dto.PublicPedidoEstadoDto;
import com.sistventas.backend.dto.PublicPedidoItemRequest;
import com.sistventas.backend.dto.PublicPedidoRequest;
import com.sistventas.backend.dto.PublicPedidoResultadoDto;
import com.sistventas.backend.dto.PublicProductoDto;
import com.sistventas.backend.dto.PublicTipDto;
import com.sistventas.backend.dto.ResenaDto;
import com.sistventas.backend.entity.EstadoVenta;
import com.sistventas.backend.exception.TiendaNoEncontradaException;
import com.sistventas.backend.exception.VentaNoEncontradaException;
import com.sistventas.backend.service.PublicTiendaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests del controller invocando los métodos directamente (sin MockMvc): la
// vidriera pública no tiene @AuthenticationPrincipal en ningún endpoint (son
// permitAll en SecurityConfig), así que alcanza con confirmar que
// PublicTiendaController delega en el service con el slug/parámetros
// correctos y arma el ResponseEntity/status esperado.
@ExtendWith(MockitoExtension.class)
class PublicTiendaControllerTest {

    private static final String SLUG = "mi-tienda";

    @Mock
    private PublicTiendaService publicTiendaService;

    @InjectMocks
    private PublicTiendaController publicTiendaController;

    @Test
    void obtenerEmpresaDevuelveOkConLaEmpresaDelService() {
        PublicEmpresaDto dto = new PublicEmpresaDto(
                "Mi Empresa", null, null, null, null, List.of(), null, null, null,
                "clasica", "claro", "vertical-1", List.of(), null, null, null, null, null,
                null, null, null
        );
        when(publicTiendaService.obtenerEmpresa(SLUG)).thenReturn(dto);

        ResponseEntity<PublicEmpresaDto> respuesta = publicTiendaController.obtenerEmpresa(SLUG);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(publicTiendaService).obtenerEmpresa(SLUG);
    }

    @Test
    void obtenerEmpresaConSlugInexistentePropagaLaExcepcionDelService() {
        when(publicTiendaService.obtenerEmpresa("no-existe")).thenThrow(new TiendaNoEncontradaException());

        assertThatThrownBy(() -> publicTiendaController.obtenerEmpresa("no-existe"))
                .isInstanceOf(TiendaNoEncontradaException.class);
    }

    @Test
    void listarProductosDevuelveOkConLaListaDelService() {
        PublicProductoDto dto = new PublicProductoDto(
                1L, "Mate", "Mate imperial", "Bebidas", null, 1L, null,
                BigDecimal.TEN, null, List.of(), 5, null, null, 0, List.of(), List.of(), List.of(), List.of()
        );
        when(publicTiendaService.listarProductos(SLUG)).thenReturn(List.of(dto));

        ResponseEntity<List<PublicProductoDto>> respuesta = publicTiendaController.listarProductos(SLUG);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(publicTiendaService).listarProductos(SLUG);
    }

    @Test
    void listarCategoriasDevuelveOkConLaListaDelService() {
        CategoriaTiendaDto dto = new CategoriaTiendaDto(1L, "Bebidas", "#a97d74", null);
        when(publicTiendaService.listarCategorias(SLUG)).thenReturn(List.of(dto));

        ResponseEntity<List<CategoriaTiendaDto>> respuesta = publicTiendaController.listarCategorias(SLUG);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(publicTiendaService).listarCategorias(SLUG);
    }

    @Test
    void listarResenasDevuelveOkConLaListaDelServiceParaElProductoIndicado() {
        ResenaDto dto = new ResenaDto(1L, "Juan", "Excelente", 5, LocalDateTime.now(), null, false);
        when(publicTiendaService.listarResenas(SLUG, 5L)).thenReturn(List.of(dto));

        ResponseEntity<List<ResenaDto>> respuesta = publicTiendaController.listarResenas(SLUG, 5L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(publicTiendaService).listarResenas(SLUG, 5L);
    }

    @Test
    void listarTipsDevuelveOkConLaListaDelService() {
        PublicTipDto dto = new PublicTipDto("Cuidado del mate", "Lavar con agua fría", null);
        when(publicTiendaService.listarTips(SLUG)).thenReturn(List.of(dto));

        ResponseEntity<List<PublicTipDto>> respuesta = publicTiendaController.listarTips(SLUG);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(publicTiendaService).listarTips(SLUG);
    }

    @Test
    void crearPedidoDevuelveCreatedConElResultadoDelService() {
        PublicPedidoRequest request = new PublicPedidoRequest(
                "Juan", "1122334455", List.of(new PublicPedidoItemRequest(1L, 2, null, null, null, null)),
                null, null, null, null
        );
        PublicPedidoResultadoDto dto = new PublicPedidoResultadoDto(50L, BigDecimal.valueOf(200));
        when(publicTiendaService.crearPedido(SLUG, request)).thenReturn(dto);

        ResponseEntity<PublicPedidoResultadoDto> respuesta = publicTiendaController.crearPedido(SLUG, request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(publicTiendaService).crearPedido(SLUG, request);
    }

    @Test
    void consultarPedidoDevuelveOkConElEstadoDelService() {
        PublicPedidoEstadoDto dto = new PublicPedidoEstadoDto(
                50L, EstadoVenta.CONFIRMADA, LocalDateTime.now(), null, BigDecimal.valueOf(200), List.of()
        );
        when(publicTiendaService.consultarPedido(SLUG, 50L, "1122334455")).thenReturn(dto);

        ResponseEntity<PublicPedidoEstadoDto> respuesta = publicTiendaController.consultarPedido(SLUG, 50L, "1122334455");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(publicTiendaService).consultarPedido(SLUG, 50L, "1122334455");
    }

    @Test
    void consultarPedidoConVentaInexistentePropagaLaExcepcionDelService() {
        when(publicTiendaService.consultarPedido(SLUG, 404L, "1122334455"))
                .thenThrow(new VentaNoEncontradaException());

        assertThatThrownBy(() -> publicTiendaController.consultarPedido(SLUG, 404L, "1122334455"))
                .isInstanceOf(VentaNoEncontradaException.class);
    }

    @Test
    void previewDescuentoComboDevuelveOkConElPreviewDelService() {
        PreviewDescuentoComboRequest request = new PreviewDescuentoComboRequest(
                List.of(new PublicPedidoItemRequest(1L, 2, null, null, null, null))
        );
        PreviewDescuentoComboDto dto = PreviewDescuentoComboDto.sinDescuento();
        when(publicTiendaService.previewDescuentoCombo(SLUG, request)).thenReturn(dto);

        ResponseEntity<PreviewDescuentoComboDto> respuesta = publicTiendaController.previewDescuentoCombo(SLUG, request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(publicTiendaService).previewDescuentoCombo(SLUG, request);
    }

    @Test
    void listarAtributosFiltroDevuelveOkConLaListaDelService() {
        AtributoFiltroDto dto = new AtributoFiltroDto(1L, 7L, "Material", List.of(new AtributoFiltroValorDto(10L, "Acero")));
        when(publicTiendaService.listarAtributosFiltro(SLUG, 7L)).thenReturn(List.of(dto));

        ResponseEntity<List<AtributoFiltroDto>> respuesta = publicTiendaController.listarAtributosFiltro(SLUG, 7L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(publicTiendaService).listarAtributosFiltro(SLUG, 7L);
    }
}
