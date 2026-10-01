package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ActualizarActivoRequest;
import com.sistventas.backend.dto.BloqueImagenUploadDto;
import com.sistventas.backend.dto.GuardarTiendaBloqueCardRequest;
import com.sistventas.backend.dto.GuardarTiendaBloqueRequest;
import com.sistventas.backend.dto.ReordenarBloquesRequest;
import com.sistventas.backend.dto.ReordenarCardsRequest;
import com.sistventas.backend.dto.TiendaBloqueCardDto;
import com.sistventas.backend.dto.TiendaBloqueDto;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.exception.BloqueTiendaInvalidoException;
import com.sistventas.backend.exception.BloqueTiendaNoEncontradoException;
import com.sistventas.backend.exception.GlobalExceptionHandler;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.TiendaBloqueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Controller invocado directamente (sin MockMvc), como UsuarioEmpresaControllerTest:
// la restricción ADMIN y las validaciones viven en el service. Acá: delegación
// con los parámetros correctos, status 201/200/204 y el mapeo 400/404/403 de
// las excepciones del service en GlobalExceptionHandler.
@ExtendWith(MockitoExtension.class)
class TiendaBloqueControllerTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private TiendaBloqueService service;

    @InjectMocks
    private TiendaBloqueController controller;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void listarDevuelve200ConLosBloques() {
        TiendaBloqueDto dto = bloqueDto(1L);
        when(service.listar(principal)).thenReturn(List.of(dto));

        ResponseEntity<List<TiendaBloqueDto>> resp = controller.listar(principal);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsExactly(dto);
    }

    @Test
    void crearDevuelve201() {
        GuardarTiendaBloqueRequest req = new GuardarTiendaBloqueRequest("t", "HOME_ANTES_FOOTER", "MITAD", true);
        TiendaBloqueDto dto = bloqueDto(1L);
        when(service.crear(req, principal)).thenReturn(dto);

        ResponseEntity<TiendaBloqueDto> resp = controller.crear(req, principal);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isEqualTo(dto);
    }

    @Test
    void actualizarDevuelve200() {
        GuardarTiendaBloqueRequest req = new GuardarTiendaBloqueRequest("t", "HOME_ANTES_FOOTER", "MITAD", true);
        when(service.actualizar(5L, req, principal)).thenReturn(bloqueDto(5L));

        assertThat(controller.actualizar(5L, req, principal).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void eliminarDevuelve204YDelega() {
        ResponseEntity<Void> resp = controller.eliminar(5L, principal);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).eliminar(5L, principal);
    }

    @Test
    void actualizarActivoDelegaConElFlagDelRequest() {
        when(service.actualizarActivo(5L, false, principal)).thenReturn(bloqueDto(5L));

        ResponseEntity<TiendaBloqueDto> resp = controller.actualizarActivo(5L, new ActualizarActivoRequest(false), principal);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).actualizarActivo(5L, false, principal);
    }

    @Test
    void reordenarDevuelve200ConLaListaDelService() {
        ReordenarBloquesRequest req = new ReordenarBloquesRequest("HOME_ANTES_FOOTER", List.of(2L, 1L));
        when(service.reordenar(req, principal)).thenReturn(List.of(bloqueDto(2L), bloqueDto(1L)));

        ResponseEntity<List<TiendaBloqueDto>> resp = controller.reordenar(req, principal);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(2);
    }

    @Test
    void crearCardDevuelve201() {
        GuardarTiendaBloqueCardRequest req = new GuardarTiendaBloqueCardRequest("/uploads/bloques/a.jpg", null, "t", null, null, null);
        TiendaBloqueCardDto dto = cardDto(8L);
        when(service.crearCard(5L, req, principal)).thenReturn(dto);

        ResponseEntity<TiendaBloqueCardDto> resp = controller.crearCard(5L, req, principal);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isEqualTo(dto);
    }

    @Test
    void actualizarCardDevuelve200() {
        GuardarTiendaBloqueCardRequest req = new GuardarTiendaBloqueCardRequest("/uploads/bloques/a.jpg", null, "t", null, null, null);
        when(service.actualizarCard(5L, 8L, req, principal)).thenReturn(cardDto(8L));

        assertThat(controller.actualizarCard(5L, 8L, req, principal).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void eliminarCardDevuelve204YDelega() {
        ResponseEntity<Void> resp = controller.eliminarCard(5L, 8L, principal);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).eliminarCard(5L, 8L, principal);
    }

    @Test
    void reordenarCardsDevuelve200() {
        ReordenarCardsRequest req = new ReordenarCardsRequest(List.of(2L, 1L));
        when(service.reordenarCards(5L, req, principal)).thenReturn(List.of(cardDto(2L), cardDto(1L)));

        assertThat(controller.reordenarCards(5L, req, principal).getBody()).hasSize(2);
    }

    @Test
    void subirImagenDevuelve200ConLaUrl() {
        MultipartFile file = mock(MultipartFile.class);
        when(service.subirImagen(file, principal)).thenReturn(new BloqueImagenUploadDto("/uploads/bloques/a.jpg"));

        ResponseEntity<BloqueImagenUploadDto> resp = controller.subirImagen(file, principal);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().imagenUrl()).isEqualTo("/uploads/bloques/a.jpg");
    }

    @Test
    void controllerPropagaLaExcepcionDeAccesoDelService() {
        UserPrincipal member = new UserPrincipal(100L, EMPRESA_ID, false, RolEmpresa.MEMBER);
        when(service.listar(member)).thenThrow(new AccesoRestringidoAdminException());

        assertThatThrownBy(() -> controller.listar(member)).isInstanceOf(AccesoRestringidoAdminException.class);
    }

    // --- GlobalExceptionHandler: 400 / 404 / 403 ---

    @Test
    void handlerMapeaInvalidoA400NoEncontradoA404YAccesoA403() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, String>> invalido = handler.handleBloqueTiendaInvalido(new BloqueTiendaInvalidoException("slot inválido"));
        ResponseEntity<Map<String, String>> noEncontrado = handler.handleBloqueTiendaNoEncontrado(new BloqueTiendaNoEncontradoException());
        ResponseEntity<Map<String, String>> acceso = handler.handleAccesoRestringidoAdmin(new AccesoRestringidoAdminException());

        assertThat(invalido.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalido.getBody()).containsEntry("error", "slot inválido");
        assertThat(noEncontrado.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(acceso.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private TiendaBloqueDto bloqueDto(Long id) {
        return new TiendaBloqueDto(id, "t", "HOME_ANTES_FOOTER", "MITAD", 0, true, List.of());
    }

    private TiendaBloqueCardDto cardDto(Long id) {
        return new TiendaBloqueCardDto(id, "/uploads/bloques/a.jpg", "VERTICAL", "t", null, 0, "NINGUNA", null);
    }
}
