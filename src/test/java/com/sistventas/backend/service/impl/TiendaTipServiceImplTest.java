package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.CrearTipRequest;
import com.sistventas.backend.dto.TipDto;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.TiendaTip;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.exception.TipNoEncontradoException;
import com.sistventas.backend.repository.TiendaTipRepository;
import com.sistventas.backend.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

// Reglas de negocio de TiendaTipServiceImpl: aislamiento multi-tenant al
// listar, y que eliminar/subir foto validen que el tip pertenezca a la
// empresa del principal. Mismo criterio que TiendaTestimonioServiceImplTest,
// solo que acá no hay canal ni cliente asociado. Repository e
// ImagenUploadValidator se mockean enteros.
@ExtendWith(MockitoExtension.class)
class TiendaTipServiceImplTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private TiendaTipRepository tiendaTipRepository;

    @Mock
    private ImagenUploadValidator imagenUploadValidator;

    @InjectMocks
    private TiendaTipServiceImpl tiendaTipService;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void listarDelegaEnElRepositoryYMapeaADto() {
        TiendaTip tip = tip(5L, "Como cebar bien", "Agua a 80 grados");
        when(tiendaTipRepository.findByEmpresaIdOrderByOrdenAscIdAsc(EMPRESA_ID)).thenReturn(List.of(tip));

        List<TipDto> resultado = tiendaTipService.listar(principal);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).titulo()).isEqualTo("Como cebar bien");
    }

    @Test
    void listarSinEmpresaLanzaExcepcion() {
        UserPrincipal sinEmpresa = new UserPrincipal(1L, null, true, null);

        assertThatThrownBy(() -> tiendaTipService.listar(sinEmpresa))
                .isInstanceOf(SinEmpresaException.class);
    }

    @Test
    void listarConRolMemberLanzaExcepcion() {
        UserPrincipal member = new UserPrincipal(1L, EMPRESA_ID, false, RolEmpresa.MEMBER);

        assertThatThrownBy(() -> tiendaTipService.listar(member))
                .isInstanceOf(AccesoRestringidoAdminException.class);
    }

    @Test
    void crearConTituloYContenidoConEspaciosLosGuardaTrimeadosConOrdenSecuencial() {
        CrearTipRequest request = new CrearTipRequest("  Limpieza del mate  ", "  Lavar con agua tibia  ");
        when(tiendaTipRepository.countByEmpresaId(EMPRESA_ID)).thenReturn(2L);
        when(tiendaTipRepository.save(any(TiendaTip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TipDto resultado = tiendaTipService.crear(request, principal);

        assertThat(resultado.titulo()).isEqualTo("Limpieza del mate");
        assertThat(resultado.contenido()).isEqualTo("Lavar con agua tibia");
        assertThat(resultado.orden()).isEqualTo(2);
    }

    @Test
    void eliminarTipDeOtraEmpresaLanzaExcepcion() {
        when(tiendaTipRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tiendaTipService.eliminar(5L, principal))
                .isInstanceOf(TipNoEncontradoException.class);

        verify(tiendaTipRepository, never()).delete(any());
    }

    @Test
    void eliminarTipPropioLoBorra() {
        TiendaTip tip = tip(5L, "Titulo", "Contenido");
        when(tiendaTipRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(tip));

        tiendaTipService.eliminar(5L, principal);

        verify(tiendaTipRepository).delete(tip);
    }

    @Test
    void actualizarFotoDeTipInexistenteLanzaExcepcion() {
        when(tiendaTipRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.empty());
        MultipartFile file = mock(MultipartFile.class);

        assertThatThrownBy(() -> tiendaTipService.actualizarFoto(5L, file, principal))
                .isInstanceOf(TipNoEncontradoException.class);

        verify(tiendaTipRepository, never()).save(any());
    }

    @Test
    void actualizarFotoDeTipExistenteGuardaLaUrlGenerada() {
        TiendaTip tip = tip(5L, "Titulo", "Contenido");
        when(tiendaTipRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(tip));
        MultipartFile file = mock(MultipartFile.class);
        when(imagenUploadValidator.validarYObtenerExtension(file)).thenReturn(".webp");
        when(tiendaTipRepository.save(any(TiendaTip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TipDto resultado = tiendaTipService.actualizarFoto(5L, file, principal);

        assertThat(resultado.fotoUrl()).startsWith("/uploads/tips/tip-5-");
        assertThat(resultado.fotoUrl()).endsWith(".webp");
    }

    private TiendaTip tip(Long id, String titulo, String contenido) {
        TiendaTip tip = new TiendaTip();
        tip.setId(id);
        tip.setEmpresaId(EMPRESA_ID);
        tip.setTitulo(titulo);
        tip.setContenido(contenido);
        return tip;
    }
}
