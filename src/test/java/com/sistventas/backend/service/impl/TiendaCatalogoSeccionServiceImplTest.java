package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarCatalogoSeccionRequest;
import com.sistventas.backend.dto.CatalogoSeccionDto;
import com.sistventas.backend.dto.CrearCatalogoSeccionRequest;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.TiendaCatalogoSeccion;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.exception.AccionNoPermitidaException;
import com.sistventas.backend.exception.CatalogoSeccionNoEncontradaException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.TiendaCatalogoSeccionRepository;
import com.sistventas.backend.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Reglas de negocio de TiendaCatalogoSeccionServiceImpl: aislamiento
// multi-tenant al listar, validación del `tipo` (solo IMPORTANTE o
// COMO_COMPRAR), orden secuencial POR TIPO (no global), y que eliminar
// valide que la sección pertenezca a la empresa del principal. Mismo
// criterio que TiendaTipServiceImplTest.
@ExtendWith(MockitoExtension.class)
class TiendaCatalogoSeccionServiceImplTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private TiendaCatalogoSeccionRepository tiendaCatalogoSeccionRepository;

    @InjectMocks
    private TiendaCatalogoSeccionServiceImpl service;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void listarDelegaEnElRepositoryYMapeaADto() {
        TiendaCatalogoSeccion seccion = seccion(5L, "IMPORTANTE", "Demora de hasta 5 días", 0);
        when(tiendaCatalogoSeccionRepository.findByEmpresaIdOrderByTipoAscOrdenAscIdAsc(EMPRESA_ID)).thenReturn(List.of(seccion));

        List<CatalogoSeccionDto> resultado = service.listar(principal);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).texto()).isEqualTo("Demora de hasta 5 días");
        assertThat(resultado.get(0).tipo()).isEqualTo("IMPORTANTE");
    }

    @Test
    void listarSinEmpresaLanzaExcepcion() {
        UserPrincipal sinEmpresa = new UserPrincipal(1L, null, true, null);

        assertThatThrownBy(() -> service.listar(sinEmpresa))
                .isInstanceOf(SinEmpresaException.class);
    }

    @Test
    void listarConRolMemberLanzaExcepcion() {
        UserPrincipal member = new UserPrincipal(1L, EMPRESA_ID, false, RolEmpresa.MEMBER);

        assertThatThrownBy(() -> service.listar(member))
                .isInstanceOf(AccesoRestringidoAdminException.class);
    }

    @Test
    void crearConTipoInvalidoLanzaExcepcionYNoGuarda() {
        CrearCatalogoSeccionRequest request = new CrearCatalogoSeccionRequest("OTRO_TIPO", "Un punto", null, null, null, null);

        assertThatThrownBy(() -> service.crear(request, principal))
                .isInstanceOf(AccionNoPermitidaException.class);

        verify(tiendaCatalogoSeccionRepository, never()).save(any());
    }

    @Test
    void crearConTextoConEspaciosLoGuardaTrimeadoConOrdenSecuencialDeSuTipo() {
        CrearCatalogoSeccionRequest request = new CrearCatalogoSeccionRequest("importante", "  Demora de hasta 5 días  ", null, null, null, null);
        when(tiendaCatalogoSeccionRepository.countByEmpresaIdAndTipo(EMPRESA_ID, "IMPORTANTE")).thenReturn(2L);
        when(tiendaCatalogoSeccionRepository.save(any(TiendaCatalogoSeccion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CatalogoSeccionDto resultado = service.crear(request, principal);

        assertThat(resultado.texto()).isEqualTo("Demora de hasta 5 días");
        assertThat(resultado.tipo()).isEqualTo("IMPORTANTE");
        assertThat(resultado.orden()).isEqualTo(2);
    }

    @Test
    void crearComoComprarUsaElContadorDeEseTipoNoElDeImportante() {
        CrearCatalogoSeccionRequest request = new CrearCatalogoSeccionRequest("COMO_COMPRAR", "Escribinos por WhatsApp", null, null, null, null);
        when(tiendaCatalogoSeccionRepository.countByEmpresaIdAndTipo(EMPRESA_ID, "COMO_COMPRAR")).thenReturn(0L);
        when(tiendaCatalogoSeccionRepository.save(any(TiendaCatalogoSeccion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CatalogoSeccionDto resultado = service.crear(request, principal);

        assertThat(resultado.orden()).isEqualTo(0);
        verify(tiendaCatalogoSeccionRepository, never()).countByEmpresaIdAndTipo(EMPRESA_ID, "IMPORTANTE");
    }

    @Test
    void crearConEstiloDeBloqueLoPersisteTalCual() {
        CrearCatalogoSeccionRequest request = new CrearCatalogoSeccionRequest(
                "importante", "Punto destacado", "centro", true, true, true);
        when(tiendaCatalogoSeccionRepository.countByEmpresaIdAndTipo(EMPRESA_ID, "IMPORTANTE")).thenReturn(0L);
        when(tiendaCatalogoSeccionRepository.save(any(TiendaCatalogoSeccion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CatalogoSeccionDto resultado = service.crear(request, principal);

        assertThat(resultado.alineacion()).isEqualTo("CENTRO");
        assertThat(resultado.negrita()).isTrue();
        assertThat(resultado.cursiva()).isTrue();
        assertThat(resultado.subrayado()).isTrue();
    }

    @Test
    void crearSinEstiloCaeAlDefaultIzquierdaSinNegritaCursivaNiSubrayado() {
        CrearCatalogoSeccionRequest request = new CrearCatalogoSeccionRequest("importante", "Punto normal", null, null, null, null);
        when(tiendaCatalogoSeccionRepository.countByEmpresaIdAndTipo(EMPRESA_ID, "IMPORTANTE")).thenReturn(0L);
        when(tiendaCatalogoSeccionRepository.save(any(TiendaCatalogoSeccion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CatalogoSeccionDto resultado = service.crear(request, principal);

        assertThat(resultado.alineacion()).isEqualTo("IZQUIERDA");
        assertThat(resultado.negrita()).isFalse();
        assertThat(resultado.cursiva()).isFalse();
        assertThat(resultado.subrayado()).isFalse();
    }

    @Test
    void crearConAlineacionInvalidaLanzaExcepcionYNoGuarda() {
        CrearCatalogoSeccionRequest request = new CrearCatalogoSeccionRequest("importante", "Punto", "ARRIBA", null, null, null);

        assertThatThrownBy(() -> service.crear(request, principal))
                .isInstanceOf(AccionNoPermitidaException.class);

        verify(tiendaCatalogoSeccionRepository, never()).save(any());
    }

    @Test
    void actualizarSeccionPropiaCambiaTextoYEstilo() {
        TiendaCatalogoSeccion existente = seccion(5L, "IMPORTANTE", "Texto viejo", 0);
        when(tiendaCatalogoSeccionRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        ActualizarCatalogoSeccionRequest request = new ActualizarCatalogoSeccionRequest("Texto nuevo", "derecha", true, false, false);

        CatalogoSeccionDto resultado = service.actualizar(5L, request, principal);

        assertThat(resultado.texto()).isEqualTo("Texto nuevo");
        assertThat(resultado.alineacion()).isEqualTo("DERECHA");
        assertThat(resultado.negrita()).isTrue();
        assertThat(resultado.cursiva()).isFalse();
    }

    @Test
    void actualizarSeccionDeOtraEmpresaLanzaExcepcion() {
        when(tiendaCatalogoSeccionRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.empty());
        ActualizarCatalogoSeccionRequest request = new ActualizarCatalogoSeccionRequest("Texto", null, null, null, null);

        assertThatThrownBy(() -> service.actualizar(5L, request, principal))
                .isInstanceOf(CatalogoSeccionNoEncontradaException.class);
    }

    @Test
    void eliminarSeccionDeOtraEmpresaLanzaExcepcion() {
        when(tiendaCatalogoSeccionRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(5L, principal))
                .isInstanceOf(CatalogoSeccionNoEncontradaException.class);

        verify(tiendaCatalogoSeccionRepository, never()).delete(any());
    }

    @Test
    void eliminarSeccionPropiaLaBorra() {
        TiendaCatalogoSeccion seccion = seccion(5L, "IMPORTANTE", "Texto", 0);
        when(tiendaCatalogoSeccionRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(seccion));

        service.eliminar(5L, principal);

        verify(tiendaCatalogoSeccionRepository).delete(seccion);
    }

    private TiendaCatalogoSeccion seccion(Long id, String tipo, String texto, int orden) {
        TiendaCatalogoSeccion seccion = new TiendaCatalogoSeccion();
        seccion.setId(id);
        seccion.setEmpresaId(EMPRESA_ID);
        seccion.setTipo(tipo);
        seccion.setTexto(texto);
        seccion.setOrden(orden);
        return seccion;
    }
}
