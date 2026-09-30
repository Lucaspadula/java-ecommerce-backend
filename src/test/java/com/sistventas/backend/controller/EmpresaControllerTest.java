package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ActualizarCatalogoConfigRequest;
import com.sistventas.backend.dto.ActualizarColorCategoriaRequest;
import com.sistventas.backend.dto.ActualizarEstiloTextoCatalogoRequest;
import com.sistventas.backend.dto.ActualizarProductoBannerImagenRequest;
import com.sistventas.backend.dto.ActualizarReglaDescuentoComboRequest;
import com.sistventas.backend.dto.ActualizarTiendaRequest;
import com.sistventas.backend.dto.BannerImagenTiendaDto;
import com.sistventas.backend.dto.CategoriaSubcategoriaDto;
import com.sistventas.backend.dto.CategoriaTiendaDto;
import com.sistventas.backend.dto.CrearReglaDescuentoComboRequest;
import com.sistventas.backend.dto.CrearTipRequest;
import com.sistventas.backend.dto.PerfilDto;
import com.sistventas.backend.dto.ReglaDescuentoComboDto;
import com.sistventas.backend.dto.TipDto;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.exception.ReglaDescuentoComboNoEncontradaException;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.PerfilService;
import com.sistventas.backend.service.ReglaDescuentoComboService;
import com.sistventas.backend.service.TiendaCategoriaService;
import com.sistventas.backend.service.TiendaTipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests del controller invocando los métodos directamente (sin MockMvc): lo
// que interesa acá es que EmpresaController delegue en cada service (son 4)
// con los parámetros correctos y arme el ResponseEntity/status esperado, no
// simular el dispatcher HTTP completo ni @AuthenticationPrincipal.
@ExtendWith(MockitoExtension.class)
class EmpresaControllerTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private PerfilService perfilService;

    @Mock
    private TiendaCategoriaService tiendaCategoriaService;

    @Mock
    private TiendaTipService tiendaTipService;

    @Mock
    private ReglaDescuentoComboService reglaDescuentoComboService;

    @InjectMocks
    private EmpresaController empresaController;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void actualizarLogoDevuelveOkConElPerfilActualizado() {
        MultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});
        PerfilDto dto = perfilDto();
        when(perfilService.actualizarLogoEmpresa(file, principal)).thenReturn(dto);

        ResponseEntity<PerfilDto> respuesta = empresaController.actualizarLogo(file, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(perfilService).actualizarLogoEmpresa(file, principal);
    }

    @Test
    void actualizarCatalogoPortadaImagenDevuelveOkConElPerfilActualizado() {
        MultipartFile file = new MockMultipartFile("file", "portada.jpg", "image/jpeg", new byte[]{1, 2, 3});
        PerfilDto dto = perfilDto();
        when(perfilService.actualizarCatalogoPortadaImagen(file, principal)).thenReturn(dto);

        ResponseEntity<PerfilDto> respuesta = empresaController.actualizarCatalogoPortadaImagen(file, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(perfilService).actualizarCatalogoPortadaImagen(file, principal);
    }

    @Test
    void quitarCatalogoPortadaImagenDevuelveOkConElPerfilActualizado() {
        PerfilDto dto = perfilDto();
        when(perfilService.quitarCatalogoPortadaImagen(principal)).thenReturn(dto);

        ResponseEntity<PerfilDto> respuesta = empresaController.quitarCatalogoPortadaImagen(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(perfilService).quitarCatalogoPortadaImagen(principal);
    }

    @Test
    void actualizarCatalogoConfigDevuelveOkConElPerfilActualizado() {
        ActualizarCatalogoConfigRequest request = new ActualizarCatalogoConfigRequest(true, "Mi catálogo", true, false, "#f7f3ec");
        PerfilDto dto = perfilDto();
        when(perfilService.actualizarCatalogoConfig(request, principal)).thenReturn(dto);

        ResponseEntity<PerfilDto> respuesta = empresaController.actualizarCatalogoConfig(request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(perfilService).actualizarCatalogoConfig(request, principal);
    }

    @Test
    void actualizarEstiloTextoCatalogoDevuelveOkConElPerfilActualizado() {
        ActualizarEstiloTextoCatalogoRequest request = new ActualizarEstiloTextoCatalogoRequest("times", 14, "#900000");
        PerfilDto dto = perfilDto();
        when(perfilService.actualizarEstiloTextoCatalogo(request, principal)).thenReturn(dto);

        ResponseEntity<PerfilDto> respuesta = empresaController.actualizarEstiloTextoCatalogo(request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(perfilService).actualizarEstiloTextoCatalogo(request, principal);
    }

    @Test
    void actualizarTiendaDevuelveOkConElPerfilActualizado() {
        ActualizarTiendaRequest request = tiendaRequest();
        PerfilDto dto = perfilDto();
        when(perfilService.actualizarTienda(request, principal)).thenReturn(dto);

        ResponseEntity<PerfilDto> respuesta = empresaController.actualizarTienda(request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(perfilService).actualizarTienda(request, principal);
    }

    @Test
    void listarBannerImagenesDevuelveOkConLaListaDelService() {
        BannerImagenTiendaDto dto = new BannerImagenTiendaDto(1L, "url", 0, "HERO", null);
        when(perfilService.listarBannerImagenes(principal)).thenReturn(List.of(dto));

        ResponseEntity<List<BannerImagenTiendaDto>> respuesta = empresaController.listarBannerImagenes(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(perfilService).listarBannerImagenes(principal);
    }

    @Test
    void agregarBannerImagenDevuelveOkConLaImagenAgregada() {
        MultipartFile file = new MockMultipartFile("file", "banner.png", "image/png", new byte[]{1, 2, 3});
        BannerImagenTiendaDto dto = new BannerImagenTiendaDto(1L, "url", 0, "VERTICAL", 5L);
        when(perfilService.agregarBannerImagen(file, "VERTICAL", 5L, principal)).thenReturn(dto);

        ResponseEntity<BannerImagenTiendaDto> respuesta =
                empresaController.agregarBannerImagen(file, "VERTICAL", 5L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(perfilService).agregarBannerImagen(file, "VERTICAL", 5L, principal);
    }

    @Test
    void eliminarBannerImagenDevuelveNoContentYDelegaEnElService() {
        ResponseEntity<Void> respuesta = empresaController.eliminarBannerImagen(1L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(respuesta.getBody()).isNull();
        verify(perfilService).eliminarBannerImagen(1L, principal);
    }

    @Test
    void actualizarProductoBannerImagenDevuelveOkConLaImagenActualizada() {
        ActualizarProductoBannerImagenRequest request = new ActualizarProductoBannerImagenRequest(7L);
        BannerImagenTiendaDto dto = new BannerImagenTiendaDto(1L, "url", 0, "HERO", 7L);
        when(perfilService.actualizarProductoBannerImagen(1L, 7L, principal)).thenReturn(dto);

        ResponseEntity<BannerImagenTiendaDto> respuesta =
                empresaController.actualizarProductoBannerImagen(1L, request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(perfilService).actualizarProductoBannerImagen(1L, 7L, principal);
    }

    @Test
    void listarCategoriasDevuelveOkConLaListaDelService() {
        CategoriaTiendaDto dto = new CategoriaTiendaDto(1L, "Bebidas", "#a97d74", null);
        when(tiendaCategoriaService.listar(principal)).thenReturn(List.of(dto));

        ResponseEntity<List<CategoriaTiendaDto>> respuesta = empresaController.listarCategorias(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(tiendaCategoriaService).listar(principal);
    }

    @Test
    void actualizarColorCategoriaDevuelveOkConLaCategoriaActualizada() {
        ActualizarColorCategoriaRequest request = new ActualizarColorCategoriaRequest("#a97d74");
        CategoriaTiendaDto dto = new CategoriaTiendaDto(1L, "Bebidas", "#a97d74", null);
        when(tiendaCategoriaService.actualizarColor(1L, request, principal)).thenReturn(dto);

        ResponseEntity<CategoriaTiendaDto> respuesta =
                empresaController.actualizarColorCategoria(1L, request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(tiendaCategoriaService).actualizarColor(1L, request, principal);
    }

    @Test
    void actualizarImagenCategoriaDevuelveOkConLaCategoriaActualizada() {
        MultipartFile file = new MockMultipartFile("file", "categoria.png", "image/png", new byte[]{1, 2, 3});
        CategoriaTiendaDto dto = new CategoriaTiendaDto(1L, "Bebidas", null, "url");
        when(tiendaCategoriaService.actualizarImagen(1L, file, principal)).thenReturn(dto);

        ResponseEntity<CategoriaTiendaDto> respuesta =
                empresaController.actualizarImagenCategoria(1L, file, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(tiendaCategoriaService).actualizarImagen(1L, file, principal);
    }

    @Test
    void eliminarImagenCategoriaDevuelveOkConLaCategoriaActualizada() {
        CategoriaTiendaDto dto = new CategoriaTiendaDto(1L, "Bebidas", null, null);
        when(tiendaCategoriaService.eliminarImagen(1L, principal)).thenReturn(dto);

        ResponseEntity<CategoriaTiendaDto> respuesta = empresaController.eliminarImagenCategoria(1L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(tiendaCategoriaService).eliminarImagen(1L, principal);
    }

    @Test
    void listarTipsDevuelveOkConLaListaDelService() {
        TipDto dto = new TipDto(1L, "Cuidado del mate", "Lavar con agua fría", 0, null);
        when(tiendaTipService.listar(principal)).thenReturn(List.of(dto));

        ResponseEntity<List<TipDto>> respuesta = empresaController.listarTips(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(tiendaTipService).listar(principal);
    }

    @Test
    void crearTipDevuelveCreatedConElTipCreado() {
        CrearTipRequest request = new CrearTipRequest("Cuidado del mate", "Lavar con agua fría");
        TipDto dto = new TipDto(1L, "Cuidado del mate", "Lavar con agua fría", 0, null);
        when(tiendaTipService.crear(request, principal)).thenReturn(dto);

        ResponseEntity<TipDto> respuesta = empresaController.crearTip(request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(tiendaTipService).crear(request, principal);
    }

    @Test
    void eliminarTipDevuelveNoContentYDelegaEnElService() {
        ResponseEntity<Void> respuesta = empresaController.eliminarTip(1L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(respuesta.getBody()).isNull();
        verify(tiendaTipService).eliminar(1L, principal);
    }

    @Test
    void actualizarFotoTipDevuelveOkConElTipActualizado() {
        MultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1, 2, 3});
        TipDto dto = new TipDto(1L, "Cuidado del mate", "Lavar con agua fría", 0, "url");
        when(tiendaTipService.actualizarFoto(1L, file, principal)).thenReturn(dto);

        ResponseEntity<TipDto> respuesta = empresaController.actualizarFotoTip(1L, file, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(tiendaTipService).actualizarFoto(1L, file, principal);
    }

    @Test
    void listarCategoriasSubcategoriasDevuelveOkConLaListaDelService() {
        CategoriaSubcategoriaDto dto = new CategoriaSubcategoriaDto(1L, "Bebidas", List.of());
        when(reglaDescuentoComboService.listarCategoriasSubcategorias(principal)).thenReturn(List.of(dto));

        ResponseEntity<List<CategoriaSubcategoriaDto>> respuesta =
                empresaController.listarCategoriasSubcategorias(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(reglaDescuentoComboService).listarCategoriasSubcategorias(principal);
    }

    @Test
    void listarReglasDescuentoComboDevuelveOkConLaListaDelService() {
        ReglaDescuentoComboDto dto = reglaDto();
        when(reglaDescuentoComboService.listar(principal)).thenReturn(List.of(dto));

        ResponseEntity<List<ReglaDescuentoComboDto>> respuesta = empresaController.listarReglasDescuentoCombo(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(reglaDescuentoComboService).listar(principal);
    }

    @Test
    void crearReglaDescuentoComboDevuelveCreatedConLaReglaCreada() {
        CrearReglaDescuentoComboRequest request =
                new CrearReglaDescuentoComboRequest(1L, null, 2L, null, BigDecimal.TEN, true);
        ReglaDescuentoComboDto dto = reglaDto();
        when(reglaDescuentoComboService.crear(request, principal)).thenReturn(dto);

        ResponseEntity<ReglaDescuentoComboDto> respuesta = empresaController.crearReglaDescuentoCombo(request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(reglaDescuentoComboService).crear(request, principal);
    }

    @Test
    void actualizarReglaDescuentoComboDevuelveOkConLaReglaActualizada() {
        ActualizarReglaDescuentoComboRequest request =
                new ActualizarReglaDescuentoComboRequest(1L, null, 2L, null, BigDecimal.TEN, false);
        ReglaDescuentoComboDto dto = reglaDto();
        when(reglaDescuentoComboService.actualizar(1L, request, principal)).thenReturn(dto);

        ResponseEntity<ReglaDescuentoComboDto> respuesta =
                empresaController.actualizarReglaDescuentoCombo(1L, request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(reglaDescuentoComboService).actualizar(1L, request, principal);
    }

    @Test
    void actualizarReglaDescuentoComboConIdInexistentePropagaLaExcepcionDelService() {
        ActualizarReglaDescuentoComboRequest request =
                new ActualizarReglaDescuentoComboRequest(1L, null, 2L, null, BigDecimal.TEN, false);
        when(reglaDescuentoComboService.actualizar(404L, request, principal))
                .thenThrow(new ReglaDescuentoComboNoEncontradaException());

        assertThatThrownBy(() -> empresaController.actualizarReglaDescuentoCombo(404L, request, principal))
                .isInstanceOf(ReglaDescuentoComboNoEncontradaException.class);
    }

    @Test
    void eliminarReglaDescuentoComboDevuelveNoContentYDelegaEnElService() {
        ResponseEntity<Void> respuesta = empresaController.eliminarReglaDescuentoCombo(1L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(respuesta.getBody()).isNull();
        verify(reglaDescuentoComboService).eliminar(1L, principal);
    }

    private PerfilDto perfilDto() {
        return new PerfilDto(
                99L, "Lucas", "lucas@test.com", RolEmpresa.ADMIN, "Mi Empresa", false,
                "logo-url", "mi-empresa", true,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                false, null, null, null,
                false, null, true, null, true, true, null, null, null, null
        );
    }

    private ActualizarTiendaRequest tiendaRequest() {
        return new ActualizarTiendaRequest(
                "mi-tienda", true, null, null, null, null, null, null, null, null,
                "clasica", "claro", null, null, null, null, null,
                false, null, null, null
        );
    }

    private ReglaDescuentoComboDto reglaDto() {
        return new ReglaDescuentoComboDto(1L, 1L, "Bebidas", null, null, 2L, "Snacks", null, null, BigDecimal.TEN, true);
    }
}
