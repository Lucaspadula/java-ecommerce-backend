package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.GuardarTiendaBloqueCardRequest;
import com.sistventas.backend.dto.GuardarTiendaBloqueRequest;
import com.sistventas.backend.dto.ReordenarBloquesRequest;
import com.sistventas.backend.dto.ReordenarCardsRequest;
import com.sistventas.backend.dto.TiendaBloqueCardDto;
import com.sistventas.backend.dto.TiendaBloqueDto;
import com.sistventas.backend.entity.Categoria;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.TiendaBloque;
import com.sistventas.backend.entity.TiendaBloqueCard;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.exception.BloqueTiendaInvalidoException;
import com.sistventas.backend.exception.BloqueTiendaNoEncontradoException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.CategoriaRepository;
import com.sistventas.backend.repository.ProductoRepository;
import com.sistventas.backend.repository.TiendaBloqueCardRepository;
import com.sistventas.backend.repository.TiendaBloqueRepository;
import com.sistventas.backend.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Reglas de negocio de TiendaBloqueServiceImpl: validaciones de slot/ancho/
// orientación/acción (400), scoping por empresa ADMIN (404 ante recursos
// ajenos), límites (30 bloques, 12 cards), orden y borrado. Mismo estilo que
// TiendaTipServiceImplTest: repositories mockeados, sin Spring.
@ExtendWith(MockitoExtension.class)
class TiendaBloqueServiceImplTest {

    private static final Long EMPRESA_ID = 1L;
    private static final Long OTRA_EMPRESA_ID = 2L;

    @Mock
    private TiendaBloqueRepository bloqueRepository;

    @Mock
    private TiendaBloqueCardRepository cardRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ImagenUploadValidator imagenUploadValidator;

    @InjectMocks
    private TiendaBloqueServiceImpl service;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    // --- acceso ---

    @Test
    void listarSinEmpresaLanzaSinEmpresa() {
        UserPrincipal sinEmpresa = new UserPrincipal(1L, null, true, null);
        assertThatThrownBy(() -> service.listar(sinEmpresa)).isInstanceOf(SinEmpresaException.class);
    }

    @Test
    void listarConRolMemberLanzaAccesoRestringido() {
        UserPrincipal member = new UserPrincipal(1L, EMPRESA_ID, false, RolEmpresa.MEMBER);
        assertThatThrownBy(() -> service.listar(member)).isInstanceOf(AccesoRestringidoAdminException.class);
    }

    @Test
    void listarTraeBloquesDeLaEmpresaConSusCardsOrdenadas() {
        TiendaBloque b = bloque(5L, EMPRESA_ID, "HOME_ANTES_FOOTER", 0);
        TiendaBloqueCard c = card(8L, 5L, 0);
        when(bloqueRepository.findByEmpresaIdOrderBySlotAscOrdenAscIdAsc(EMPRESA_ID)).thenReturn(List.of(b));
        when(cardRepository.findByBloqueIdInOrderByOrdenAscIdAsc(List.of(5L))).thenReturn(List.of(c));

        List<TiendaBloqueDto> resultado = service.listar(principal);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).cards()).hasSize(1);
        assertThat(resultado.get(0).cards().get(0).id()).isEqualTo(8L);
    }

    // --- crear bloque: validaciones (E1, E2, E3) ---

    @Test
    void crearBloqueValidoQuedaActivoEnLaEmpresaYAlFinalDelSlot() {
        when(bloqueRepository.countByEmpresaId(EMPRESA_ID)).thenReturn(3L);
        when(bloqueRepository.countByEmpresaIdAndSlot(EMPRESA_ID, "HOME_ANTES_FOOTER")).thenReturn(2L);
        when(bloqueRepository.save(any(TiendaBloque.class))).thenAnswer(i -> i.getArgument(0));

        TiendaBloqueDto dto = service.crear(
                new GuardarTiendaBloqueRequest("  Novedades  ", "HOME_ANTES_FOOTER", "MITAD", null), principal);

        assertThat(dto.titulo()).isEqualTo("Novedades");
        assertThat(dto.slot()).isEqualTo("HOME_ANTES_FOOTER");
        assertThat(dto.ancho()).isEqualTo("MITAD");
        assertThat(dto.activo()).isTrue();
        assertThat(dto.orden()).isEqualTo(2);
    }

    @Test
    void crearBloqueConSlotInvalidoLanzaInvalidoYNoPersiste() {
        assertThatThrownBy(() -> service.crear(
                new GuardarTiendaBloqueRequest("x", "HOME_ARRIBA", "MITAD", true), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);
        verify(bloqueRepository, never()).save(any());
    }

    @Test
    void crearBloqueConAnchoInvalidoLanzaInvalido() {
        assertThatThrownBy(() -> service.crear(
                new GuardarTiendaBloqueRequest("x", "HOME_ANTES_FOOTER", "CUARTO", true), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);
    }

    @Test
    void crearBloqueSinSlotLanzaInvalido() {
        assertThatThrownBy(() -> service.crear(
                new GuardarTiendaBloqueRequest("x", null, "MITAD", true), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);
    }

    @Test
    void crearBloqueConTituloEnBlancoLoGuardaNull() {
        when(bloqueRepository.countByEmpresaId(EMPRESA_ID)).thenReturn(0L);
        when(bloqueRepository.countByEmpresaIdAndSlot(EMPRESA_ID, "HOME_ANTES_FOOTER")).thenReturn(0L);
        when(bloqueRepository.save(any(TiendaBloque.class))).thenAnswer(i -> i.getArgument(0));

        TiendaBloqueDto dto = service.crear(
                new GuardarTiendaBloqueRequest("   ", "HOME_ANTES_FOOTER", "COMPLETO", true), principal);

        assertThat(dto.titulo()).isNull();
    }

    @Test
    void crearBloqueSuperandoElLimiteDe30LanzaInvalido() {
        when(bloqueRepository.countByEmpresaId(EMPRESA_ID)).thenReturn(30L);

        assertThatThrownBy(() -> service.crear(
                new GuardarTiendaBloqueRequest("x", "HOME_ANTES_FOOTER", "MITAD", true), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);
        verify(bloqueRepository, never()).save(any());
    }

    // --- actualizar bloque: scoping, cambio de slot ---

    @Test
    void actualizarBloqueAjenoLanzaNoEncontrado() {
        when(bloqueRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(10L,
                new GuardarTiendaBloqueRequest("x", "HOME_ANTES_FOOTER", "MITAD", true), principal))
                .isInstanceOf(BloqueTiendaNoEncontradoException.class);
        verify(bloqueRepository, never()).save(any());
    }

    @Test
    void actualizarBloqueConCambioDeSlotLoMueveAlFinalDelSlotNuevo() {
        TiendaBloque b = bloque(10L, EMPRESA_ID, "HOME_ANTES_FOOTER", 0);
        when(bloqueRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(b));
        when(bloqueRepository.countByEmpresaIdAndSlot(EMPRESA_ID, "CATALOGO_SOBRE_GRILLA")).thenReturn(4L);
        when(bloqueRepository.save(any(TiendaBloque.class))).thenAnswer(i -> i.getArgument(0));
        when(cardRepository.findByBloqueIdOrderByOrdenAscIdAsc(10L)).thenReturn(List.of());

        TiendaBloqueDto dto = service.actualizar(10L,
                new GuardarTiendaBloqueRequest("t", "CATALOGO_SOBRE_GRILLA", "TERCIO", true), principal);

        assertThat(dto.slot()).isEqualTo("CATALOGO_SOBRE_GRILLA");
        assertThat(dto.orden()).isEqualTo(4);
    }

    @Test
    void actualizarBloqueSinCambioDeSlotConservaElOrden() {
        TiendaBloque b = bloque(10L, EMPRESA_ID, "HOME_ANTES_FOOTER", 3);
        when(bloqueRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(b));
        when(bloqueRepository.save(any(TiendaBloque.class))).thenAnswer(i -> i.getArgument(0));
        when(cardRepository.findByBloqueIdOrderByOrdenAscIdAsc(10L)).thenReturn(List.of());

        TiendaBloqueDto dto = service.actualizar(10L,
                new GuardarTiendaBloqueRequest("t", "HOME_ANTES_FOOTER", "TERCIO", false), principal);

        assertThat(dto.orden()).isEqualTo(3);
        assertThat(dto.activo()).isFalse();
    }

    // --- activo / eliminar (E10) ---

    @Test
    void actualizarActivoCambiaElFlag() {
        TiendaBloque b = bloque(10L, EMPRESA_ID, "HOME_ANTES_FOOTER", 0);
        when(bloqueRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(b));
        when(bloqueRepository.save(any(TiendaBloque.class))).thenAnswer(i -> i.getArgument(0));
        when(cardRepository.findByBloqueIdOrderByOrdenAscIdAsc(10L)).thenReturn(List.of());

        TiendaBloqueDto dto = service.actualizarActivo(10L, false, principal);

        assertThat(dto.activo()).isFalse();
    }

    @Test
    void eliminarBorraPrimeroLasCardsYDespuesElBloque() {
        TiendaBloque b = bloque(10L, EMPRESA_ID, "HOME_ANTES_FOOTER", 0);
        when(bloqueRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.of(b));

        service.eliminar(10L, principal);

        InOrder orden = inOrder(cardRepository, bloqueRepository);
        orden.verify(cardRepository).deleteByBloqueId(10L);
        orden.verify(bloqueRepository).delete(b);
    }

    @Test
    void eliminarBloqueAjenoLanzaNoEncontradoYNoBorraNada() {
        when(bloqueRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(10L, principal))
                .isInstanceOf(BloqueTiendaNoEncontradoException.class);
        verify(cardRepository, never()).deleteByBloqueId(any());
        verify(bloqueRepository, never()).delete(any());
    }

    // --- reorden de bloques (E7, E11) ---

    @Test
    void reordenarBloquesAsignaOrdenPorIndiceDeLaListaCompleta() {
        TiendaBloque a = bloque(1L, EMPRESA_ID, "HOME_ANTES_FOOTER", 0);
        TiendaBloque b = bloque(2L, EMPRESA_ID, "HOME_ANTES_FOOTER", 1);
        TiendaBloque c = bloque(3L, EMPRESA_ID, "HOME_ANTES_FOOTER", 2);
        when(bloqueRepository.findByEmpresaIdAndSlotOrderByOrdenAscIdAsc(EMPRESA_ID, "HOME_ANTES_FOOTER"))
                .thenReturn(List.of(a, b, c));
        when(cardRepository.findByBloqueIdInOrderByOrdenAscIdAsc(any())).thenReturn(List.of());

        List<TiendaBloqueDto> resultado = service.reordenar(
                new ReordenarBloquesRequest("HOME_ANTES_FOOTER", List.of(3L, 1L, 2L)), principal);

        assertThat(c.getOrden()).isZero();
        assertThat(a.getOrden()).isEqualTo(1);
        assertThat(b.getOrden()).isEqualTo(2);
        assertThat(resultado).extracting(TiendaBloqueDto::id).containsExactly(3L, 1L, 2L);
    }

    @Test
    void reordenarConIdDeOtraEmpresaLanzaNoEncontradoYNoCambiaNada() {
        TiendaBloque a = bloque(1L, EMPRESA_ID, "HOME_ANTES_FOOTER", 0);
        when(bloqueRepository.findByEmpresaIdAndSlotOrderByOrdenAscIdAsc(EMPRESA_ID, "HOME_ANTES_FOOTER"))
                .thenReturn(List.of(a));

        assertThatThrownBy(() -> service.reordenar(
                new ReordenarBloquesRequest("HOME_ANTES_FOOTER", List.of(1L, 77L)), principal))
                .isInstanceOf(BloqueTiendaNoEncontradoException.class);
        assertThat(a.getOrden()).isZero();
        verify(bloqueRepository, never()).saveAll(any());
    }

    @Test
    void reordenarConListaIncompletaLanzaInvalido() {
        TiendaBloque a = bloque(1L, EMPRESA_ID, "HOME_ANTES_FOOTER", 0);
        TiendaBloque b = bloque(2L, EMPRESA_ID, "HOME_ANTES_FOOTER", 1);
        when(bloqueRepository.findByEmpresaIdAndSlotOrderByOrdenAscIdAsc(EMPRESA_ID, "HOME_ANTES_FOOTER"))
                .thenReturn(List.of(a, b));

        assertThatThrownBy(() -> service.reordenar(
                new ReordenarBloquesRequest("HOME_ANTES_FOOTER", List.of(2L)), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);
    }

    @Test
    void reordenarConSlotInvalidoLanzaInvalido() {
        assertThatThrownBy(() -> service.reordenar(
                new ReordenarBloquesRequest("NOPE", List.of(1L)), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);
    }

    // --- cards: validaciones de acción (E4, E5, E6) ---

    @Test
    void crearCardSinAccionQuedaEnNingunaYSinValor() {
        stubBloqueConCards(10L, 0L);

        TiendaBloqueCardDto dto = service.crearCard(10L, cardReq("/uploads/bloques/a.jpg", null, null, null), principal);

        assertThat(dto.accion()).isEqualTo("NINGUNA");
        assertThat(dto.accionValor()).isNull();
        assertThat(dto.orientacion()).isEqualTo("VERTICAL");
    }

    @Test
    void crearCardNingunaConValorLoLimpia() {
        stubBloqueConCards(10L, 0L);

        TiendaBloqueCardDto dto = service.crearCard(10L, cardReq("/uploads/bloques/a.jpg", "NINGUNA", "algo", null), principal);

        assertThat(dto.accionValor()).isNull();
    }

    @Test
    void crearCardConOrientacionOAccionInvalidaLanzaInvalido() {
        stubBloqueSolo(10L);

        assertThatThrownBy(() -> service.crearCard(10L,
                new GuardarTiendaBloqueCardRequest("/uploads/bloques/a.jpg", "DIAGONAL", "t", "x", "NINGUNA", null), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);
        assertThatThrownBy(() -> service.crearCard(10L, cardReq("/uploads/bloques/a.jpg", "VOLAR", null, null), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);
    }

    @Test
    void crearCardSinImagenOConImagenFueraDeUploadsLanzaInvalido() {
        stubBloqueSolo(10L);

        assertThatThrownBy(() -> service.crearCard(10L, cardReq(null, "NINGUNA", null, null), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);
        assertThatThrownBy(() -> service.crearCard(10L, cardReq("http://evil.com/a.jpg", "NINGUNA", null, null), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);
        assertThatThrownBy(() -> service.crearCard(10L, cardReq("/uploads/../etc/passwd", "NINGUNA", null, null), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);
    }

    @Test
    void crearCardUrlAceptaHttpYHttps() {
        stubBloqueConCards(10L, 0L);

        TiendaBloqueCardDto dto = service.crearCard(10L, cardReq("/uploads/bloques/a.jpg", "URL", "https://ejemplo.com/x", null), principal);

        assertThat(dto.accion()).isEqualTo("URL");
        assertThat(dto.accionValor()).isEqualTo("https://ejemplo.com/x");
        verify(cardRepository).save(any(TiendaBloqueCard.class));
    }

    @Test
    void crearCardUrlRechazaJavascriptFtpDataRelativasYVacias() {
        stubBloqueSolo(10L);

        for (String mala : new String[]{"javascript:alert(1)", "ftp://x.com", "data:text/html,hola", "/relativa", "ejemplo.com", "http://", "  ", null}) {
            assertThatThrownBy(() -> service.crearCard(10L, cardReq("/uploads/bloques/a.jpg", "URL", mala, null), principal))
                    .as("valor %s", mala)
                    .isInstanceOf(BloqueTiendaInvalidoException.class);
        }
    }

    @Test
    void crearCardProductoDeLaEmpresaGuardaElId() {
        stubBloqueConCards(10L, 0L);
        Producto p = new Producto();
        p.setId(7L);
        p.setEmpresaId(EMPRESA_ID);
        when(productoRepository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.of(p));

        TiendaBloqueCardDto dto = service.crearCard(10L, cardReq("/uploads/bloques/a.jpg", "PRODUCTO", "7", null), principal);

        assertThat(dto.accion()).isEqualTo("PRODUCTO");
        assertThat(dto.accionValor()).isEqualTo("7");
    }

    @Test
    void crearCardProductoAjenoNoNumericoOAusenteLanzaInvalido() {
        stubBloqueSolo(10L);
        when(productoRepository.findByIdAndEmpresaId(9L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearCard(10L, cardReq("/uploads/bloques/a.jpg", "PRODUCTO", "9", null), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);
        assertThatThrownBy(() -> service.crearCard(10L, cardReq("/uploads/bloques/a.jpg", "PRODUCTO", "abc", null), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);
        assertThatThrownBy(() -> service.crearCard(10L, cardReq("/uploads/bloques/a.jpg", "PRODUCTO", null, null), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);
    }

    @Test
    void crearCardCategoriaDeLaEmpresaGuardaElIdYAjenaLanzaInvalido() {
        stubBloqueConCards(10L, 0L);
        Categoria cat = new Categoria();
        cat.setId(4L);
        when(categoriaRepository.findByIdAndEmpresaId(4L, EMPRESA_ID)).thenReturn(Optional.of(cat));
        when(categoriaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.empty());

        TiendaBloqueCardDto dto = service.crearCard(10L, cardReq("/uploads/bloques/a.jpg", "CATEGORIA", "4", null), principal);

        assertThat(dto.accionValor()).isEqualTo("4");
        assertThatThrownBy(() -> service.crearCard(10L, cardReq("/uploads/bloques/a.jpg", "CATEGORIA", "5", null), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);
    }

    @Test
    void crearCardModalSinTituloNiTextoLanzaInvalidoYConUnoDeLosDosAcepta() {
        stubBloqueConCards(10L, 0L);

        assertThatThrownBy(() -> service.crearCard(10L,
                new GuardarTiendaBloqueCardRequest("/uploads/bloques/a.jpg", "VERTICAL", " ", " ", "MODAL", null), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);

        TiendaBloqueCardDto dto = service.crearCard(10L,
                new GuardarTiendaBloqueCardRequest("/uploads/bloques/a.jpg", "VERTICAL", null, "Detalle", "MODAL", "ignorado"), principal);
        assertThat(dto.accion()).isEqualTo("MODAL");
        assertThat(dto.accionValor()).isNull();
    }

    // --- cards: límite, scoping, orden, borrado ---

    @Test
    void crearCardSuperandoElLimiteDe12LanzaInvalido() {
        stubBloqueConCards(10L, 12L);

        assertThatThrownBy(() -> service.crearCard(10L, cardReq("/uploads/bloques/a.jpg", "NINGUNA", null, null), principal))
                .isInstanceOf(BloqueTiendaInvalidoException.class);
        verify(cardRepository, never()).save(any());
    }

    @Test
    void crearCardEnBloqueAjenoLanzaNoEncontrado() {
        when(bloqueRepository.findByIdAndEmpresaId(10L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearCard(10L, cardReq("/uploads/bloques/a.jpg", "NINGUNA", null, null), principal))
                .isInstanceOf(BloqueTiendaNoEncontradoException.class);
    }

    @Test
    void crearCardSeAgregaAlFinalDeLasCardsDelBloque() {
        stubBloqueConCards(10L, 5L);

        TiendaBloqueCardDto dto = service.crearCard(10L, cardReq("/uploads/bloques/a.jpg", "NINGUNA", null, null), principal);

        assertThat(dto.orden()).isEqualTo(5);
    }

    @Test
    void actualizarCardInexistenteEnElBloqueLanzaNoEncontrado() {
        stubBloqueSolo(10L);
        when(cardRepository.findByIdAndBloqueId(99L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarCard(10L, 99L, cardReq("/uploads/bloques/a.jpg", "NINGUNA", null, null), principal))
                .isInstanceOf(BloqueTiendaNoEncontradoException.class);
    }

    @Test
    void actualizarCardAplicaLosCambiosValidados() {
        stubBloqueSolo(10L);
        TiendaBloqueCard c = card(8L, 10L, 2);
        when(cardRepository.findByIdAndBloqueId(8L, 10L)).thenReturn(Optional.of(c));
        when(cardRepository.save(any(TiendaBloqueCard.class))).thenAnswer(i -> i.getArgument(0));

        TiendaBloqueCardDto dto = service.actualizarCard(10L, 8L,
                new GuardarTiendaBloqueCardRequest("/uploads/bloques/b.jpg", "HORIZONTAL", " Nuevo ", null, "URL", "http://a.com"), principal);

        assertThat(dto.imagenUrl()).isEqualTo("/uploads/bloques/b.jpg");
        assertThat(dto.orientacion()).isEqualTo("HORIZONTAL");
        assertThat(dto.titulo()).isEqualTo("Nuevo");
        assertThat(dto.orden()).isEqualTo(2);
    }

    @Test
    void eliminarCardBorraSoloSiPerteneceAlBloqueDeLaEmpresa() {
        stubBloqueSolo(10L);
        TiendaBloqueCard c = card(8L, 10L, 0);
        when(cardRepository.findByIdAndBloqueId(8L, 10L)).thenReturn(Optional.of(c));

        service.eliminarCard(10L, 8L, principal);

        verify(cardRepository).delete(c);
    }

    @Test
    void reordenarCardsAsignaOrdenPorIndice() {
        stubBloqueSolo(10L);
        TiendaBloqueCard a = card(1L, 10L, 0);
        TiendaBloqueCard b = card(2L, 10L, 1);
        when(cardRepository.findByBloqueIdOrderByOrdenAscIdAsc(10L)).thenReturn(List.of(a, b));

        List<TiendaBloqueCardDto> resultado = service.reordenarCards(10L, new ReordenarCardsRequest(List.of(2L, 1L)), principal);

        assertThat(b.getOrden()).isZero();
        assertThat(a.getOrden()).isEqualTo(1);
        assertThat(resultado).extracting(TiendaBloqueCardDto::id).containsExactly(2L, 1L);
    }

    @Test
    void reordenarCardsConIdAjenoLanzaNoEncontrado() {
        stubBloqueSolo(10L);
        TiendaBloqueCard a = card(1L, 10L, 0);
        when(cardRepository.findByBloqueIdOrderByOrdenAscIdAsc(10L)).thenReturn(List.of(a));

        assertThatThrownBy(() -> service.reordenarCards(10L, new ReordenarCardsRequest(List.of(1L, 55L)), principal))
                .isInstanceOf(BloqueTiendaNoEncontradoException.class);
    }

    // --- helpers ---

    private void stubBloqueSolo(Long id) {
        when(bloqueRepository.findByIdAndEmpresaId(id, EMPRESA_ID))
                .thenReturn(Optional.of(bloque(id, EMPRESA_ID, "HOME_ANTES_FOOTER", 0)));
    }

    private void stubBloqueConCards(Long id, long cantidadCards) {
        stubBloqueSolo(id);
        lenient().when(cardRepository.countByBloqueId(id)).thenReturn(cantidadCards);
        lenient().when(cardRepository.save(any(TiendaBloqueCard.class))).thenAnswer(i -> i.getArgument(0));
    }

    private GuardarTiendaBloqueCardRequest cardReq(String imagenUrl, String accion, String valor, String texto) {
        return new GuardarTiendaBloqueCardRequest(imagenUrl, null, "Titulo", texto, accion, valor);
    }

    private TiendaBloque bloque(Long id, Long empresaId, String slot, int orden) {
        TiendaBloque b = new TiendaBloque();
        b.setId(id);
        b.setEmpresaId(empresaId);
        b.setSlot(slot);
        b.setAncho("COMPLETO");
        b.setOrden(orden);
        b.setActivo(true);
        return b;
    }

    private TiendaBloqueCard card(Long id, Long bloqueId, int orden) {
        TiendaBloqueCard c = new TiendaBloqueCard();
        c.setId(id);
        c.setBloqueId(bloqueId);
        c.setImagenUrl("/uploads/bloques/x.jpg");
        c.setOrientacion("VERTICAL");
        c.setAccion("NINGUNA");
        c.setOrden(orden);
        return c;
    }
}
