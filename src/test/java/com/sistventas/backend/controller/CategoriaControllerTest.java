package com.sistventas.backend.controller;

import com.sistventas.backend.dto.CategoriaDto;
import com.sistventas.backend.dto.CrearCategoriaRequest;
import com.sistventas.backend.dto.CrearSubcategoriaRequest;
import com.sistventas.backend.dto.SubcategoriaDto;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.exception.CategoriaNoEncontradaException;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.CategoriaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests del controller invocando los métodos directamente (sin MockMvc): lo
// que interesa acá es que CategoriaController delegue en el service con los
// parámetros correctos y arme el ResponseEntity con el status HTTP esperado,
// no simular el dispatcher HTTP completo ni @AuthenticationPrincipal.
@ExtendWith(MockitoExtension.class)
class CategoriaControllerTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private CategoriaController categoriaController;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void listarDevuelveOkConLaListaDelService() {
        CategoriaDto dto = new CategoriaDto(1L, "Bebidas");
        when(categoriaService.listar(principal)).thenReturn(List.of(dto));

        ResponseEntity<List<CategoriaDto>> respuesta = categoriaController.listar(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(categoriaService).listar(principal);
    }

    @Test
    void crearDevuelveCreatedConLaCategoriaCreada() {
        CrearCategoriaRequest request = new CrearCategoriaRequest("Mates");
        CategoriaDto dto = new CategoriaDto(10L, "Mates");
        when(categoriaService.crear(request, principal)).thenReturn(dto);

        ResponseEntity<CategoriaDto> respuesta = categoriaController.crear(request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(categoriaService).crear(request, principal);
    }

    @Test
    void listarSubcategoriasDevuelveOkConLaListaDelService() {
        SubcategoriaDto dto = new SubcategoriaDto(2L, "Gaseosas");
        when(categoriaService.listarSubcategorias(1L, principal)).thenReturn(List.of(dto));

        ResponseEntity<List<SubcategoriaDto>> respuesta = categoriaController.listarSubcategorias(1L, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(categoriaService).listarSubcategorias(1L, principal);
    }

    @Test
    void listarSubcategoriasConCategoriaInexistentePropagaLaExcepcionDelService() {
        when(categoriaService.listarSubcategorias(404L, principal))
                .thenThrow(new CategoriaNoEncontradaException());

        assertThatThrownBy(() -> categoriaController.listarSubcategorias(404L, principal))
                .isInstanceOf(CategoriaNoEncontradaException.class);
    }

    @Test
    void crearSubcategoriaDevuelveCreatedConLaSubcategoriaCreada() {
        CrearSubcategoriaRequest request = new CrearSubcategoriaRequest("Gaseosas");
        SubcategoriaDto dto = new SubcategoriaDto(3L, "Gaseosas");
        when(categoriaService.crearSubcategoria(1L, request, principal)).thenReturn(dto);

        ResponseEntity<SubcategoriaDto> respuesta = categoriaController.crearSubcategoria(1L, request, principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(categoriaService).crearSubcategoria(1L, request, principal);
    }

    @Test
    void crearSubcategoriaConCategoriaInexistentePropagaLaExcepcionDelService() {
        CrearSubcategoriaRequest request = new CrearSubcategoriaRequest("Gaseosas");
        when(categoriaService.crearSubcategoria(404L, request, principal))
                .thenThrow(new CategoriaNoEncontradaException());

        assertThatThrownBy(() -> categoriaController.crearSubcategoria(404L, request, principal))
                .isInstanceOf(CategoriaNoEncontradaException.class);
    }
}
