package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.CrearTestimonioRequest;
import com.sistventas.backend.dto.TestimonioDto;
import com.sistventas.backend.entity.CanalTestimonio;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.TiendaTestimonio;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.exception.TestimonioNoEncontradoException;
import com.sistventas.backend.repository.TiendaTestimonioRepository;
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

// Reglas de negocio de TiendaTestimonioServiceImpl: canal opcional al crear,
// aislamiento multi-tenant al listar, y que eliminar/subir foto validen que
// el testimonio pertenezca a la empresa del principal (nunca confiar en un id
// que venga del cliente). Repository e ImagenUploadValidator se mockean
// enteros.
@ExtendWith(MockitoExtension.class)
class TiendaTestimonioServiceImplTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private TiendaTestimonioRepository tiendaTestimonioRepository;

    @Mock
    private ImagenUploadValidator imagenUploadValidator;

    @InjectMocks
    private TiendaTestimonioServiceImpl tiendaTestimonioService;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void listarDelegaEnElRepositoryYMapeaADto() {
        TiendaTestimonio testimonio = testimonio(5L, "Juan", "Excelente atencion", CanalTestimonio.WHATSAPP);
        when(tiendaTestimonioRepository.findByEmpresaIdOrderByOrdenAscIdAsc(EMPRESA_ID)).thenReturn(List.of(testimonio));

        List<TestimonioDto> resultado = tiendaTestimonioService.listar(principal);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).clienteNombre()).isEqualTo("Juan");
        assertThat(resultado.get(0).canal()).isEqualTo(CanalTestimonio.WHATSAPP);
    }

    @Test
    void listarSinEmpresaLanzaExcepcion() {
        UserPrincipal sinEmpresa = new UserPrincipal(1L, null, true, null);

        assertThatThrownBy(() -> tiendaTestimonioService.listar(sinEmpresa))
                .isInstanceOf(SinEmpresaException.class);
    }

    @Test
    void listarConRolMemberLanzaExcepcion() {
        UserPrincipal member = new UserPrincipal(1L, EMPRESA_ID, false, RolEmpresa.MEMBER);

        assertThatThrownBy(() -> tiendaTestimonioService.listar(member))
                .isInstanceOf(AccesoRestringidoAdminException.class);
    }

    @Test
    void crearConCanalNuloPersisteElTestimonioSinCanal() {
        CrearTestimonioRequest request = new CrearTestimonioRequest("Ana", "Muy buen servicio", null);
        when(tiendaTestimonioRepository.countByEmpresaId(EMPRESA_ID)).thenReturn(0L);
        when(tiendaTestimonioRepository.save(any(TiendaTestimonio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestimonioDto resultado = tiendaTestimonioService.crear(request, principal);

        assertThat(resultado.canal()).isNull();
        assertThat(resultado.clienteNombre()).isEqualTo("Ana");
        assertThat(resultado.orden()).isZero();
    }

    @Test
    void crearConNombreYComentarioConEspaciosLosGuardaTrimeados() {
        CrearTestimonioRequest request = new CrearTestimonioRequest("  Pedro  ", "  Recomendado  ", CanalTestimonio.INSTAGRAM);
        when(tiendaTestimonioRepository.countByEmpresaId(EMPRESA_ID)).thenReturn(3L);
        when(tiendaTestimonioRepository.save(any(TiendaTestimonio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestimonioDto resultado = tiendaTestimonioService.crear(request, principal);

        assertThat(resultado.clienteNombre()).isEqualTo("Pedro");
        assertThat(resultado.comentario()).isEqualTo("Recomendado");
        assertThat(resultado.orden()).isEqualTo(3);
        assertThat(resultado.canal()).isEqualTo(CanalTestimonio.INSTAGRAM);
    }

    @Test
    void eliminarTestimonioDeOtraEmpresaLanzaExcepcion() {
        when(tiendaTestimonioRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tiendaTestimonioService.eliminar(5L, principal))
                .isInstanceOf(TestimonioNoEncontradoException.class);

        verify(tiendaTestimonioRepository, never()).delete(any());
    }

    @Test
    void eliminarTestimonioPropioLoBorra() {
        TiendaTestimonio testimonio = testimonio(5L, "Juan", "Genial", null);
        when(tiendaTestimonioRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(testimonio));

        tiendaTestimonioService.eliminar(5L, principal);

        verify(tiendaTestimonioRepository).delete(testimonio);
    }

    @Test
    void actualizarFotoDeTestimonioInexistenteLanzaExcepcion() {
        when(tiendaTestimonioRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.empty());
        MultipartFile file = mock(MultipartFile.class);

        assertThatThrownBy(() -> tiendaTestimonioService.actualizarFoto(5L, file, principal))
                .isInstanceOf(TestimonioNoEncontradoException.class);

        verify(tiendaTestimonioRepository, never()).save(any());
    }

    @Test
    void actualizarFotoDeTestimonioExistenteGuardaLaUrlGenerada() {
        TiendaTestimonio testimonio = testimonio(5L, "Juan", "Genial", null);
        when(tiendaTestimonioRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(testimonio));
        MultipartFile file = mock(MultipartFile.class);
        when(imagenUploadValidator.validarYObtenerExtension(file)).thenReturn(".jpg");
        when(tiendaTestimonioRepository.save(any(TiendaTestimonio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestimonioDto resultado = tiendaTestimonioService.actualizarFoto(5L, file, principal);

        assertThat(resultado.fotoUrl()).startsWith("/uploads/testimonios/testimonio-5-");
        assertThat(resultado.fotoUrl()).endsWith(".jpg");
    }

    private TiendaTestimonio testimonio(Long id, String clienteNombre, String comentario, CanalTestimonio canal) {
        TiendaTestimonio testimonio = new TiendaTestimonio();
        testimonio.setId(id);
        testimonio.setEmpresaId(EMPRESA_ID);
        testimonio.setClienteNombre(clienteNombre);
        testimonio.setComentario(comentario);
        testimonio.setCanal(canal);
        return testimonio;
    }
}
