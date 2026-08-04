package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarColorCategoriaRequest;
import com.sistventas.backend.dto.CategoriaTiendaDto;
import com.sistventas.backend.entity.Categoria;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.TiendaCategoria;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.exception.ArchivoInvalidoException;
import com.sistventas.backend.exception.CategoriaNoEncontradaException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.CategoriaRepository;
import com.sistventas.backend.repository.TiendaCategoriaRepository;
import com.sistventas.backend.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Reglas de negocio de TiendaCategoriaServiceImpl: aislamiento multi-tenant al
// listar, upsert por (empresaId, categoriaId) al personalizar color/imagen, y
// que una categoría de otra empresa (o inexistente) nunca sea editable. El
// repository se mockea entero, no interesa JPA.
@ExtendWith(MockitoExtension.class)
class TiendaCategoriaServiceImplTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private TiendaCategoriaRepository tiendaCategoriaRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private TiendaCategoriaServiceImpl tiendaCategoriaService;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void listarTraeSoloLasCategoriasDeLaEmpresaConSuConfiguracionONullSiNoTiene() {
        Categoria mates = categoria(1L, "Mates");
        Categoria bombillas = categoria(2L, "Bombillas");
        when(categoriaRepository.findByEmpresaIdOrderByNombreAsc(EMPRESA_ID)).thenReturn(List.of(mates, bombillas));

        TiendaCategoria configurada = tiendaCategoria(EMPRESA_ID, 1L, "#ABCDEF", "/uploads/categorias/x.png");
        when(tiendaCategoriaRepository.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of(configurada));

        List<CategoriaTiendaDto> resultado = tiendaCategoriaService.listar(principal);

        assertThat(resultado).hasSize(2);
        CategoriaTiendaDto dtoMates = resultado.stream().filter(d -> d.categoriaId().equals(1L)).findFirst().orElseThrow();
        assertThat(dtoMates.color()).isEqualTo("#ABCDEF");
        assertThat(dtoMates.imagenUrl()).isEqualTo("/uploads/categorias/x.png");

        CategoriaTiendaDto dtoBombillas = resultado.stream().filter(d -> d.categoriaId().equals(2L)).findFirst().orElseThrow();
        assertThat(dtoBombillas.color()).isNull();
        assertThat(dtoBombillas.imagenUrl()).isNull();
    }

    @Test
    void listarSinEmpresaLanzaExcepcion() {
        UserPrincipal sinEmpresa = new UserPrincipal(1L, null, true, null);

        assertThatThrownBy(() -> tiendaCategoriaService.listar(sinEmpresa))
                .isInstanceOf(SinEmpresaException.class);
    }

    @Test
    void listarConRolMemberLanzaExcepcion() {
        UserPrincipal member = new UserPrincipal(1L, EMPRESA_ID, false, RolEmpresa.MEMBER);

        assertThatThrownBy(() -> tiendaCategoriaService.listar(member))
                .isInstanceOf(AccesoRestringidoAdminException.class);
    }

    @Test
    void actualizarColorDeCategoriaInexistenteOAjenaLanzaExcepcion() {
        when(categoriaRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.empty());
        ActualizarColorCategoriaRequest request = new ActualizarColorCategoriaRequest("#a97d74");

        assertThatThrownBy(() -> tiendaCategoriaService.actualizarColor(10L, request, principal))
                .isInstanceOf(CategoriaNoEncontradaException.class);

        verify(tiendaCategoriaRepository, never()).save(any());
    }

    @Test
    void actualizarColorSinConfiguracionPreviaCreaYGuardaUpsert() {
        Categoria mates = categoria(1L, "Mates");
        when(categoriaRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(mates));
        when(tiendaCategoriaRepository.findByEmpresaIdAndCategoriaId(EMPRESA_ID, 1L)).thenReturn(Optional.empty());
        when(tiendaCategoriaRepository.save(any(TiendaCategoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActualizarColorCategoriaRequest request = new ActualizarColorCategoriaRequest("#a97d74");
        CategoriaTiendaDto resultado = tiendaCategoriaService.actualizarColor(1L, request, principal);

        assertThat(resultado.color()).isEqualTo("#a97d74");

        ArgumentCaptor<TiendaCategoria> captor = ArgumentCaptor.forClass(TiendaCategoria.class);
        verify(tiendaCategoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getEmpresaId()).isEqualTo(EMPRESA_ID);
        assertThat(captor.getValue().getCategoriaId()).isEqualTo(1L);
        assertThat(captor.getValue().getColor()).isEqualTo("#a97d74");
    }

    @Test
    void actualizarImagenConContentTypeInvalidoLanzaExcepcionYNoGuarda() {
        Categoria mates = categoria(1L, "Mates");
        when(categoriaRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(mates));

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/jpeg");

        assertThatThrownBy(() -> tiendaCategoriaService.actualizarImagen(1L, file, principal))
                .isInstanceOf(ArchivoInvalidoException.class);

        verify(tiendaCategoriaRepository, never()).save(any());
    }

    @Test
    void actualizarImagenValidaGuardaLaUrlGenerada() {
        Categoria mates = categoria(1L, "Mates");
        when(categoriaRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(mates));
        when(tiendaCategoriaRepository.findByEmpresaIdAndCategoriaId(EMPRESA_ID, 1L)).thenReturn(Optional.empty());
        when(tiendaCategoriaRepository.save(any(TiendaCategoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");

        CategoriaTiendaDto resultado = tiendaCategoriaService.actualizarImagen(1L, file, principal);

        assertThat(resultado.imagenUrl()).startsWith("/uploads/categorias/categoria-" + EMPRESA_ID + "-");
        assertThat(resultado.imagenUrl()).endsWith(".png");
    }

    @Test
    void eliminarImagenDeCategoriaAjenaLanzaExcepcion() {
        when(categoriaRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tiendaCategoriaService.eliminarImagen(1L, principal))
                .isInstanceOf(CategoriaNoEncontradaException.class);

        verify(tiendaCategoriaRepository, never()).save(any());
    }

    @Test
    void eliminarImagenDejaLaUrlEnNull() {
        Categoria mates = categoria(1L, "Mates");
        when(categoriaRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(mates));
        TiendaCategoria configurada = tiendaCategoria(EMPRESA_ID, 1L, "#ABCDEF", "/uploads/categorias/x.png");
        when(tiendaCategoriaRepository.findByEmpresaIdAndCategoriaId(EMPRESA_ID, 1L)).thenReturn(Optional.of(configurada));
        when(tiendaCategoriaRepository.save(any(TiendaCategoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoriaTiendaDto resultado = tiendaCategoriaService.eliminarImagen(1L, principal);

        assertThat(resultado.imagenUrl()).isNull();
        assertThat(configurada.getImagenUrl()).isNull();
    }

    private Categoria categoria(Long id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setEmpresaId(EMPRESA_ID);
        categoria.setNombre(nombre);
        return categoria;
    }

    private TiendaCategoria tiendaCategoria(Long empresaId, Long categoriaId, String color, String imagenUrl) {
        TiendaCategoria tiendaCategoria = new TiendaCategoria();
        tiendaCategoria.setEmpresaId(empresaId);
        tiendaCategoria.setCategoriaId(categoriaId);
        tiendaCategoria.setColor(color);
        tiendaCategoria.setImagenUrl(imagenUrl);
        return tiendaCategoria;
    }
}
