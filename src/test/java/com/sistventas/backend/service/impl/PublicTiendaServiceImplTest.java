package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.AtributoFiltroDto;
import com.sistventas.backend.dto.CategoriaTiendaDto;
import com.sistventas.backend.dto.ClienteLoginResponse;
import com.sistventas.backend.dto.FotoUploadDto;
import com.sistventas.backend.dto.PreviewDescuentoComboDto;
import com.sistventas.backend.dto.PreviewDescuentoComboRequest;
import com.sistventas.backend.dto.PublicComponenteDto;
import com.sistventas.backend.dto.PublicCategoriaMenuDto;
import com.sistventas.backend.dto.PublicEmpresaDto;
import com.sistventas.backend.dto.PublicPedidoEstadoDto;
import com.sistventas.backend.dto.PublicPedidoItemRequest;
import com.sistventas.backend.dto.PublicPedidoRequest;
import com.sistventas.backend.dto.PublicPedidoResultadoDto;
import com.sistventas.backend.dto.PublicProductoDto;
import com.sistventas.backend.dto.PublicTipDto;
import com.sistventas.backend.dto.RegistrarClienteRequest;
import com.sistventas.backend.entity.AtributoFiltro;
import com.sistventas.backend.entity.AtributoFiltroValor;
import com.sistventas.backend.entity.Categoria;
import com.sistventas.backend.entity.Cliente;
import com.sistventas.backend.entity.Empresa;
import com.sistventas.backend.entity.EstadoVenta;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ProductoComponente;
import com.sistventas.backend.entity.ProductoFoto;
import com.sistventas.backend.entity.ProductoGrabado;
import com.sistventas.backend.entity.ProductoVariante;
import com.sistventas.backend.entity.ReglaDescuentoCombo;
import com.sistventas.backend.entity.Resena;
import com.sistventas.backend.entity.Subcategoria;
import com.sistventas.backend.entity.TiendaCategoria;
import com.sistventas.backend.entity.TiendaTip;
import com.sistventas.backend.entity.Venta;
import com.sistventas.backend.dto.ResenaDto;
import com.sistventas.backend.exception.AccionNoPermitidaException;
import com.sistventas.backend.exception.CategoriaNoEncontradaException;
import com.sistventas.backend.exception.ClienteYaRegistradoException;
import com.sistventas.backend.exception.CuponInvalidoException;
import com.sistventas.backend.exception.ProductoNoEncontradoException;
import com.sistventas.backend.exception.StockInsuficienteException;
import com.sistventas.backend.exception.TiendaNoEncontradaException;
import com.sistventas.backend.exception.VentaNoEncontradaException;
import com.sistventas.backend.repository.AtributoFiltroRepository;
import com.sistventas.backend.repository.AtributoFiltroValorRepository;
import com.sistventas.backend.repository.CategoriaRepository;
import com.sistventas.backend.repository.SubcategoriaRepository;
import com.sistventas.backend.security.GoogleTokenVerifier;
import com.sistventas.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.sistventas.backend.repository.ClienteRepository;
import com.sistventas.backend.repository.EmpresaRepository;
import com.sistventas.backend.repository.ProductoRepository;
import com.sistventas.backend.repository.ReglaDescuentoComboRepository;
import com.sistventas.backend.repository.ResenaRepository;
import com.sistventas.backend.repository.TiendaBannerImagenRepository;
import com.sistventas.backend.repository.TiendaCategoriaRepository;
import com.sistventas.backend.repository.TiendaTipRepository;
import com.sistventas.backend.repository.VentaEstadoHistorialRepository;
import com.sistventas.backend.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests de la vidriera pública (PublicTiendaServiceImpl): exposición de
// tiendaFuente/tiendaTema en el DTO público de empresa, mapeo de componentes
// de kit a PublicComponenteDto (sin precio/costo) y el 404 de tienda no
// encontrada. Todos los repositories se mockean: acá no interesa JPA.
@ExtendWith(MockitoExtension.class)
class PublicTiendaServiceImplTest {

    private static final Long EMPRESA_ID = 20L;
    private static final String SLUG = "mi-tienda";

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private VentaEstadoHistorialRepository ventaEstadoHistorialRepository;

    @Mock
    private TiendaCategoriaRepository tiendaCategoriaRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private SubcategoriaRepository subcategoriaRepository;

    @Mock
    private AtributoFiltroRepository atributoFiltroRepository;

    @Mock
    private AtributoFiltroValorRepository atributoFiltroValorRepository;

    @Mock
    private TiendaBannerImagenRepository tiendaBannerImagenRepository;

    @Mock
    private ResenaRepository resenaRepository;

    @Mock
    private TiendaTipRepository tiendaTipRepository;

    @Mock
    private StockDisponibleCalculator stockDisponibleCalculator;

    @Mock
    private ReglaDescuentoComboRepository reglaDescuentoComboRepository;

    @Mock
    private CalculadorDescuentoComboService calculadorDescuentoComboService;

    @Mock
    private ImagenUploadValidator imagenUploadValidator;

    // Se usa la implementación REAL (no un mock puro sin stub) en los tests
    // nuevos de fotos vía @InjectMocks + esta declaración: al ser un
    // @Component sin dependencias, Mockito lo inyecta igual, pero acá se
    // sobreescribe con la instancia real cuando el test necesita la regla de
    // negocio de verdad (ver setUp de los tests de fotos más abajo).
    @Mock
    private ProductoFotoResolver productoFotoResolver;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @Mock
    private com.sistventas.backend.repository.TiendaBloqueRepository tiendaBloqueRepository;

    @Mock
    private com.sistventas.backend.repository.TiendaBloqueCardRepository tiendaBloqueCardRepository;

    @InjectMocks
    private PublicTiendaServiceImpl publicTiendaService;

    // --- listarBloques (bloques-tienda): solo activos, orden, destino resuelto, sin N+1 ---

    @Test
    void listarBloquesDevuelveSoloLosActivosEnElOrdenDelRepositoryConSusCardsOrdenadas() {
        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa()));
        com.sistventas.backend.entity.TiendaBloque b0 = bloque(1L, "HOME_ANTES_FOOTER", 0);
        com.sistventas.backend.entity.TiendaBloque b1 = bloque(2L, "HOME_ANTES_FOOTER", 1);
        when(tiendaBloqueRepository.findByEmpresaIdAndActivoTrueOrderBySlotAscOrdenAscIdAsc(EMPRESA_ID))
                .thenReturn(List.of(b0, b1));
        when(tiendaBloqueCardRepository.findByBloqueIdInOrderByOrdenAscIdAsc(List.of(1L, 2L)))
                .thenReturn(List.of(cardBloque(10L, 1L, "NINGUNA", null), cardBloque(11L, 1L, "NINGUNA", null),
                        cardBloque(12L, 2L, "NINGUNA", null)));

        List<com.sistventas.backend.dto.PublicTiendaBloqueDto> resultado = publicTiendaService.listarBloques(SLUG);

        assertThat(resultado).extracting(com.sistventas.backend.dto.PublicTiendaBloqueDto::id).containsExactly(1L, 2L);
        assertThat(resultado.get(0).cards()).extracting(com.sistventas.backend.dto.PublicTiendaBloqueCardDto::id)
                .containsExactly(10L, 11L);
        assertThat(resultado.get(1).cards()).hasSize(1);
        // Una query de bloques y una de cards, sin N+1.
        verify(tiendaBloqueCardRepository).findByBloqueIdInOrderByOrdenAscIdAsc(List.of(1L, 2L));
    }

    @Test
    void listarBloquesSinBloquesActivosDevuelveListaVaciaSinConsultarCards() {
        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa()));
        when(tiendaBloqueRepository.findByEmpresaIdAndActivoTrueOrderBySlotAscOrdenAscIdAsc(EMPRESA_ID))
                .thenReturn(List.of());

        assertThat(publicTiendaService.listarBloques(SLUG)).isEmpty();
        verify(tiendaBloqueCardRepository, never()).findByBloqueIdInOrderByOrdenAscIdAsc(any());
    }

    @Test
    void listarBloquesResuelveDestinoDeCategoriaPorNombreYProductoPorId() {
        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa()));
        when(tiendaBloqueRepository.findByEmpresaIdAndActivoTrueOrderBySlotAscOrdenAscIdAsc(EMPRESA_ID))
                .thenReturn(List.of(bloque(1L, "HOME_ANTES_FOOTER", 0)));
        when(tiendaBloqueCardRepository.findByBloqueIdInOrderByOrdenAscIdAsc(List.of(1L))).thenReturn(List.of(
                cardBloque(10L, 1L, "CATEGORIA", "4"),
                cardBloque(11L, 1L, "PRODUCTO", "7"),
                cardBloque(12L, 1L, "URL", "https://ejemplo.com"),
                cardBloque(13L, 1L, "MODAL", null)));
        Categoria cat = new Categoria();
        cat.setId(4L);
        cat.setEmpresaId(EMPRESA_ID);
        cat.setNombre("Mates");
        Producto prod = new Producto();
        prod.setId(7L);
        prod.setEmpresaId(EMPRESA_ID);
        when(categoriaRepository.findAllById(any())).thenReturn(List.of(cat));
        when(productoRepository.findAllById(any())).thenReturn(List.of(prod));

        var cards = publicTiendaService.listarBloques(SLUG).get(0).cards();

        assertThat(cards.get(0).accion()).isEqualTo("CATEGORIA");
        assertThat(cards.get(0).destino()).isEqualTo("Mates");
        assertThat(cards.get(1).accion()).isEqualTo("PRODUCTO");
        assertThat(cards.get(1).destino()).isEqualTo("7");
        assertThat(cards.get(2).destino()).isEqualTo("https://ejemplo.com");
        assertThat(cards.get(3).accion()).isEqualTo("MODAL");
        assertThat(cards.get(3).destino()).isNull();
    }

    @Test
    void listarBloquesBajaANingunaSiLaReferenciaYaNoExisteOEsDeOtraEmpresa() {
        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa()));
        when(tiendaBloqueRepository.findByEmpresaIdAndActivoTrueOrderBySlotAscOrdenAscIdAsc(EMPRESA_ID))
                .thenReturn(List.of(bloque(1L, "HOME_ANTES_FOOTER", 0)));
        when(tiendaBloqueCardRepository.findByBloqueIdInOrderByOrdenAscIdAsc(List.of(1L))).thenReturn(List.of(
                cardBloque(10L, 1L, "CATEGORIA", "4"),
                cardBloque(11L, 1L, "PRODUCTO", "7")));
        Categoria ajena = new Categoria();
        ajena.setId(4L);
        ajena.setEmpresaId(999L);
        ajena.setNombre("Ajena");
        when(categoriaRepository.findAllById(any())).thenReturn(List.of(ajena));
        when(productoRepository.findAllById(any())).thenReturn(List.of());

        var cards = publicTiendaService.listarBloques(SLUG).get(0).cards();

        assertThat(cards.get(0).accion()).isEqualTo("NINGUNA");
        assertThat(cards.get(0).destino()).isNull();
        assertThat(cards.get(1).accion()).isEqualTo("NINGUNA");
        assertThat(cards.get(1).destino()).isNull();
    }

    private com.sistventas.backend.entity.TiendaBloque bloque(Long id, String slot, int orden) {
        com.sistventas.backend.entity.TiendaBloque b = new com.sistventas.backend.entity.TiendaBloque();
        b.setId(id);
        b.setEmpresaId(EMPRESA_ID);
        b.setSlot(slot);
        b.setAncho("COMPLETO");
        b.setOrden(orden);
        b.setActivo(true);
        return b;
    }

    private com.sistventas.backend.entity.TiendaBloqueCard cardBloque(Long id, Long bloqueId, String accion, String valor) {
        com.sistventas.backend.entity.TiendaBloqueCard c = new com.sistventas.backend.entity.TiendaBloqueCard();
        c.setId(id);
        c.setBloqueId(bloqueId);
        c.setImagenUrl("/uploads/bloques/x.jpg");
        c.setOrientacion("VERTICAL");
        c.setAccion(accion);
        c.setAccionValor(valor);
        return c;
    }

    @Test
    void obtenerEmpresaExponeTiendaFuenteYTiendaTemaDeLaEmpresa() {
        Empresa empresa = empresa();
        empresa.setTiendaFuente("moderna");
        empresa.setTiendaTema("oscuro");
        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(tiendaBannerImagenRepository.findByEmpresaIdAndTipoOrderByOrden(EMPRESA_ID, "HERO")).thenReturn(List.of());
        when(tiendaBannerImagenRepository.findByEmpresaIdAndTipoOrderByOrden(EMPRESA_ID, "VERTICAL")).thenReturn(List.of());

        PublicEmpresaDto resultado = publicTiendaService.obtenerEmpresa(SLUG);

        assertThat(resultado.tiendaFuente()).isEqualTo("moderna");
        assertThat(resultado.tiendaTema()).isEqualTo("oscuro");
    }

    @Test
    void obtenerEmpresaConSlugNoEncontradoLanzaTiendaNoEncontradaException() {
        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicTiendaService.obtenerEmpresa(SLUG))
                .isInstanceOf(TiendaNoEncontradaException.class);
    }

    @Test
    void listarProductosIncluyeComponentesParaUnKitYListaVaciaParaUnProductoSimple() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();

        Producto mate = producto(1L, "Mate imperial", categoria);
        Producto kit = producto(2L, "Combo mate + termo", categoria);
        ProductoComponente componente = new ProductoComponente();
        componente.setProducto(kit);
        componente.setComponenteProducto(mate);
        componente.setCantidad(2);
        kit.getComponentes().add(componente);

        Producto simple = producto(3L, "Bombilla suelta", categoria);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(kit, simple));
        when(resenaRepository.findByEmpresaIdOrderByFechaDesc(EMPRESA_ID)).thenReturn(List.of());
        when(stockDisponibleCalculator.calcular(kit)).thenReturn(3);
        when(stockDisponibleCalculator.calcular(simple)).thenReturn(5);

        List<PublicProductoDto> resultado = publicTiendaService.listarProductos(SLUG);

        PublicProductoDto kitDto = resultado.stream().filter(p -> p.id().equals(2L)).findFirst().orElseThrow();
        PublicProductoDto simpleDto = resultado.stream().filter(p -> p.id().equals(3L)).findFirst().orElseThrow();

        assertThat(kitDto.componentes()).hasSize(1);
        PublicComponenteDto componenteDto = kitDto.componentes().get(0);
        assertThat(componenteDto.nombre()).isEqualTo("Mate imperial");
        assertThat(componenteDto.cantidad()).isEqualTo(2);

        assertThat(simpleDto.componentes()).isEmpty();

        // El DTO público de componente nunca expone precio ni costo: solo
        // nombre + cantidad (ver comentario en PublicComponenteDto).
        assertThat(PublicComponenteDto.class.getRecordComponents()).hasSize(2);
        assertThat(List.of(PublicComponenteDto.class.getRecordComponents()))
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactlyInAnyOrder("nombre", "cantidad");
    }

    @Test
    void listarProductosNoIncluyeProductosSinStockDisponible() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto sinStock = producto(4L, "Sin stock", categoria);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(sinStock));
        when(resenaRepository.findByEmpresaIdOrderByFechaDesc(EMPRESA_ID)).thenReturn(List.of());
        when(stockDisponibleCalculator.calcular(sinStock)).thenReturn(0);

        List<PublicProductoDto> resultado = publicTiendaService.listarProductos(SLUG);

        assertThat(resultado).isEmpty();
    }

    // --- crearPedido: el flujo de checkout completo (plata + stock real) ---

    @Test
    void crearPedidoConStockSuficienteCreaLaVentaYCalculaElTotalCorrectamente() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(30L, "Mate imperial", categoria);
        producto.setPrecioVenta(new BigDecimal("1500.00"));

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(30L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(stockDisponibleCalculator.calcular(producto)).thenReturn(10);
        when(clienteRepository.findByEmpresaIdAndTelefono(EMPRESA_ID, "1122334455"))
                .thenReturn(Optional.of(cliente(55L, "Juan", "1122334455")));
        when(calculadorDescuentoComboService.calcular(anyList(), anyList()))
                .thenReturn(ResultadoDescuentoCombo.sinDescuento());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta venta = invocation.getArgument(0);
            venta.setId(900L);
            return venta;
        });

        PublicPedidoRequest request = new PublicPedidoRequest(
                "Juan", "1122334455",
                List.of(new PublicPedidoItemRequest(30L, 3, null, null, null, null)),
                null, null, null, null);

        PublicPedidoResultadoDto resultado = publicTiendaService.crearPedido(SLUG, request);

        assertThat(resultado.ventaId()).isEqualTo(900L);
        assertThat(resultado.total()).isEqualByComparingTo(new BigDecimal("4500.00"));

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(captor.capture());
        Venta ventaGuardada = captor.getValue();
        assertThat(ventaGuardada.getEmpresaId()).isEqualTo(EMPRESA_ID);
        assertThat(ventaGuardada.getClienteId()).isEqualTo(55L);
        assertThat(ventaGuardada.getEstado()).isEqualTo(EstadoVenta.PRESUPUESTO);
        assertThat(ventaGuardada.getItems()).hasSize(1);
        assertThat(ventaGuardada.getItems().get(0).getCantidad()).isEqualTo(3);
        assertThat(ventaGuardada.getItems().get(0).getSubtotal()).isEqualByComparingTo(new BigDecimal("4500.00"));
    }

    @Test
    void crearPedidoConStockInsuficienteLanzaExcepcionYNoCreaNada() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(31L, "Termo acero", categoria);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(31L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(stockDisponibleCalculator.calcular(producto)).thenReturn(1);
        when(clienteRepository.findByEmpresaIdAndTelefono(EMPRESA_ID, "1122334455"))
                .thenReturn(Optional.of(cliente(56L, "Ana", "1122334455")));

        PublicPedidoRequest request = new PublicPedidoRequest(
                "Ana", "1122334455",
                List.of(new PublicPedidoItemRequest(31L, 5, null, null, null, null)),
                null, null, null, null);

        assertThatThrownBy(() -> publicTiendaService.crearPedido(SLUG, request))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("Termo acero");

        verify(ventaRepository, never()).save(any());
    }

    @Test
    void crearPedidoConProductoConVariantesSinElegirColorLanzaExcepcion() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(32L, "Mate imperial", categoria);
        producto.getVariantes().add(variante(producto, 1L, "Rojo"));

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(32L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(clienteRepository.findByEmpresaIdAndTelefono(EMPRESA_ID, "1122334455"))
                .thenReturn(Optional.of(cliente(57L, "Pedro", "1122334455")));

        PublicPedidoRequest request = new PublicPedidoRequest(
                "Pedro", "1122334455",
                List.of(new PublicPedidoItemRequest(32L, 1, null, null, null, null)),
                null, null, null, null);

        assertThatThrownBy(() -> publicTiendaService.crearPedido(SLUG, request))
                .isInstanceOf(AccionNoPermitidaException.class)
                .hasMessage("Elegí un color para Mate imperial");

        verify(ventaRepository, never()).save(any());
    }

    @Test
    void crearPedidoConCuponValidoAplicaElDescuentoSobreElTotal() {
        Empresa empresa = empresa();
        empresa.setTiendaCuponCodigo("PROMO10");
        empresa.setTiendaCuponPorcentaje(new BigDecimal("10"));
        Categoria categoria = categoria();
        Producto producto = producto(33L, "Bombilla", categoria);
        producto.setPrecioVenta(new BigDecimal("1000.00"));

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(33L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(stockDisponibleCalculator.calcular(producto)).thenReturn(5);
        when(clienteRepository.findByEmpresaIdAndTelefono(EMPRESA_ID, "1122334455"))
                .thenReturn(Optional.of(cliente(58L, "Sol", "1122334455")));
        when(calculadorDescuentoComboService.calcular(anyList(), anyList()))
                .thenReturn(ResultadoDescuentoCombo.sinDescuento());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // El código se manda en minúsculas y con espacios: el matcheo real es
        // case-insensitive + trim (ver comparación en crearPedido).
        PublicPedidoRequest request = new PublicPedidoRequest(
                "Sol", "1122334455",
                List.of(new PublicPedidoItemRequest(33L, 1, null, null, null, null)),
                " promo10 ", null, null, null);

        PublicPedidoResultadoDto resultado = publicTiendaService.crearPedido(SLUG, request);

        assertThat(resultado.total()).isEqualByComparingTo(new BigDecimal("900.00"));
    }

    @Test
    void crearPedidoConCuponInvalidoLanzaExcepcionYNoCreaNada() {
        Empresa empresa = empresa();
        empresa.setTiendaCuponCodigo("PROMO10");
        empresa.setTiendaCuponPorcentaje(new BigDecimal("10"));
        Categoria categoria = categoria();
        Producto producto = producto(34L, "Bombilla", categoria);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(34L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(stockDisponibleCalculator.calcular(producto)).thenReturn(5);
        when(clienteRepository.findByEmpresaIdAndTelefono(EMPRESA_ID, "1122334455"))
                .thenReturn(Optional.of(cliente(59L, "Mora", "1122334455")));
        when(calculadorDescuentoComboService.calcular(anyList(), anyList()))
                .thenReturn(ResultadoDescuentoCombo.sinDescuento());

        PublicPedidoRequest request = new PublicPedidoRequest(
                "Mora", "1122334455",
                List.of(new PublicPedidoItemRequest(34L, 1, null, null, null, null)),
                "codigo-que-no-existe", null, null, null);

        assertThatThrownBy(() -> publicTiendaService.crearPedido(SLUG, request))
                .isInstanceOf(CuponInvalidoException.class);

        verify(ventaRepository, never()).save(any());
    }

    // --- consultarPedido: seguimiento público por ventaId + teléfono ---

    @Test
    void consultarPedidoConTelefonoQueNoCoincideLanzaVentaNoEncontrada() {
        Empresa empresa = empresa();
        Venta venta = new Venta();
        venta.setId(500L);
        venta.setEmpresaId(EMPRESA_ID);
        venta.setClienteId(60L);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(ventaRepository.findByIdAndEmpresaId(500L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(clienteRepository.findByIdAndEmpresaId(60L, EMPRESA_ID))
                .thenReturn(Optional.of(cliente(60L, "Nico", "1100001111")));

        assertThatThrownBy(() -> publicTiendaService.consultarPedido(SLUG, 500L, "9999999999"))
                .isInstanceOf(VentaNoEncontradaException.class);
    }

    @Test
    void consultarPedidoConVentaIdInexistenteLanzaVentaNoEncontrada() {
        Empresa empresa = empresa();
        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(ventaRepository.findByIdAndEmpresaId(999L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicTiendaService.consultarPedido(SLUG, 999L, "1100001111"))
                .isInstanceOf(VentaNoEncontradaException.class);
    }

    @Test
    void consultarPedidoConTelefonoCoincidenteDevuelveElEstadoDeLaVenta() {
        Empresa empresa = empresa();
        Venta venta = new Venta();
        venta.setId(501L);
        venta.setEmpresaId(EMPRESA_ID);
        venta.setClienteId(61L);
        venta.setEstado(EstadoVenta.CONFIRMADA);
        venta.setTotal(new BigDecimal("500.00"));

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(ventaRepository.findByIdAndEmpresaId(501L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(clienteRepository.findByIdAndEmpresaId(61L, EMPRESA_ID))
                .thenReturn(Optional.of(cliente(61L, "Vale", "1100002222")));
        when(ventaEstadoHistorialRepository.findByVentaIdOrderByFechaDescIdDesc(501L)).thenReturn(List.of());

        PublicPedidoEstadoDto resultado = publicTiendaService.consultarPedido(SLUG, 501L, "1100002222");

        assertThat(resultado.ventaId()).isEqualTo(501L);
        assertThat(resultado.estado()).isEqualTo(EstadoVenta.CONFIRMADA);
        assertThat(resultado.total()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    // --- listarTips / listarResenas: scoping por empresa ---

    @Test
    void listarTipsTraeSoloLosMapeadosAlDtoPublico() {
        Empresa empresa = empresa();
        TiendaTip tip = new TiendaTip();
        tip.setEmpresaId(EMPRESA_ID);
        tip.setTitulo("Cuidado del mate");
        tip.setContenido("Lavalo con agua fría, nunca con detergente");

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(tiendaTipRepository.findByEmpresaIdOrderByOrdenAscIdAsc(EMPRESA_ID)).thenReturn(List.of(tip));

        List<PublicTipDto> resultado = publicTiendaService.listarTips(SLUG);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).titulo()).isEqualTo("Cuidado del mate");
        assertThat(resultado.get(0).contenido()).isEqualTo("Lavalo con agua fría, nunca con detergente");
        verify(tiendaTipRepository).findByEmpresaIdOrderByOrdenAscIdAsc(EMPRESA_ID);
    }

    @Test
    void listarResenasTraeSoloLasDelProductoDeLaEmpresaCorrecta() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(40L, "Mate imperial", categoria);
        Resena resena = new Resena();
        resena.setId(1L);
        resena.setProductoId(40L);
        resena.setEmpresaId(EMPRESA_ID);
        resena.setClienteNombre("Caro");
        resena.setComentario("Buenísimo");
        resena.setFecha(LocalDateTime.now());

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(40L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(resenaRepository.findByProductoIdOrderByFechaDesc(40L)).thenReturn(List.of(resena));

        List<ResenaDto> resultado = publicTiendaService.listarResenas(SLUG, 40L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).clienteNombre()).isEqualTo("Caro");
        assertThat(resultado.get(0).comentario()).isEqualTo("Buenísimo");
    }

    // --- subirFotoGrabado ---

    @Test
    void subirFotoGrabadoConSlugInexistenteLanzaTiendaNoEncontrada() {
        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicTiendaService.subirFotoGrabado(SLUG, mock(MultipartFile.class)))
                .isInstanceOf(TiendaNoEncontradaException.class);
    }

    @Test
    void subirFotoGrabadoConArchivoValidoDevuelveUrlConLaExtensionValidada() {
        Empresa empresa = empresa();
        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        MultipartFile file = mock(MultipartFile.class);
        when(imagenUploadValidator.validarYObtenerExtension(file)).thenReturn(".jpg");

        FotoUploadDto resultado = publicTiendaService.subirFotoGrabado(SLUG, file);

        assertThat(resultado.fotoUrl()).startsWith("/uploads/grabados/");
        assertThat(resultado.fotoUrl()).endsWith(".jpg");
    }

    @Test
    void subirFotoGrabadoConFalloDeIOLanzaUncheckedIOException() throws IOException {
        Empresa empresa = empresa();
        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        MultipartFile file = mock(MultipartFile.class);
        when(imagenUploadValidator.validarYObtenerExtension(file)).thenReturn(".jpg");
        doThrow(new IOException("disco lleno")).when(file).transferTo(any(java.nio.file.Path.class));

        assertThatThrownBy(() -> publicTiendaService.subirFotoGrabado(SLUG, file))
                .isInstanceOf(java.io.UncheckedIOException.class);
    }

    // --- listarCategorias ---

    @Test
    void listarCategoriasSoloIncluyeCategoriasRealesConColorOImagenConfigurados() {
        Empresa empresa = empresa();
        Categoria mates = categoria();
        Categoria decoracion = new Categoria();
        decoracion.setId(2L);
        decoracion.setEmpresaId(EMPRESA_ID);
        decoracion.setNombre("Decoración");
        Producto productoMates = producto(1L, "Mate", mates);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(productoMates));
        when(categoriaRepository.findByEmpresaIdOrderByNombreAsc(EMPRESA_ID)).thenReturn(List.of(mates, decoracion));

        TiendaCategoria tcMatesConColor = tiendaCategoria(1L, mates.getId(), "#FF0000", null);
        // "Decoración" no tiene ningún producto activo (categoriaIdsReales no
        // la incluye): tiene que quedar afuera aunque tenga color configurado.
        TiendaCategoria tcDecoracionConColor = tiendaCategoria(2L, decoracion.getId(), "#00FF00", null);
        when(tiendaCategoriaRepository.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of(tcMatesConColor, tcDecoracionConColor));

        List<CategoriaTiendaDto> resultado = publicTiendaService.listarCategorias(SLUG);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).categoriaId()).isEqualTo(mates.getId());
        assertThat(resultado.get(0).color()).isEqualTo("#FF0000");
    }

    @Test
    void listarCategoriasExcluyeLasSinColorNiImagenConfigurados() {
        Empresa empresa = empresa();
        Categoria mates = categoria();
        Producto productoMates = producto(1L, "Mate", mates);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(productoMates));
        when(categoriaRepository.findByEmpresaIdOrderByNombreAsc(EMPRESA_ID)).thenReturn(List.of(mates));
        when(tiendaCategoriaRepository.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of(tiendaCategoria(1L, mates.getId(), null, null)));

        List<CategoriaTiendaDto> resultado = publicTiendaService.listarCategorias(SLUG);

        assertThat(resultado).isEmpty();
    }

    // --- listarCategoriasMenu ---

    @Test
    void listarCategoriasMenuArmaElArbolConImagenesYSoloSubcategoriasConProductosActivos() {
        Empresa empresa = empresa();
        Categoria mates = categoria();

        Subcategoria bombillas = new Subcategoria();
        bombillas.setId(10L);
        bombillas.setCategoriaId(mates.getId());
        bombillas.setNombre("Bombillas");
        bombillas.setImagenUrl("/uploads/categorias/bombillas.png");

        Subcategoria sinProductos = new Subcategoria();
        sinProductos.setId(11L);
        sinProductos.setCategoriaId(mates.getId());
        sinProductos.setNombre("Sin productos");

        Producto productoMate = producto(1L, "Mate", mates);
        productoMate.setSubcategoria(bombillas);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(productoMate));
        when(categoriaRepository.findByEmpresaIdOrderByNombreAsc(EMPRESA_ID)).thenReturn(List.of(mates));
        when(subcategoriaRepository.findByCategoriaIdOrderByNombreAsc(mates.getId()))
                .thenReturn(List.of(bombillas, sinProductos));
        when(tiendaCategoriaRepository.findByEmpresaId(EMPRESA_ID))
                .thenReturn(List.of(tiendaCategoria(1L, mates.getId(), null, "/uploads/categorias/mates.png")));

        List<PublicCategoriaMenuDto> resultado = publicTiendaService.listarCategoriasMenu(SLUG);

        assertThat(resultado).hasSize(1);
        PublicCategoriaMenuDto categoriaDto = resultado.get(0);
        assertThat(categoriaDto.categoriaId()).isEqualTo(mates.getId());
        assertThat(categoriaDto.imagenUrl()).isEqualTo("/uploads/categorias/mates.png");
        assertThat(categoriaDto.subcategorias()).hasSize(1);
        assertThat(categoriaDto.subcategorias().get(0).id()).isEqualTo(bombillas.getId());
        assertThat(categoriaDto.subcategorias().get(0).imagenUrl()).isEqualTo("/uploads/categorias/bombillas.png");
    }

    @Test
    void listarCategoriasMenuIncluyeCategoriaSinImagenConfigurada() {
        Empresa empresa = empresa();
        Categoria mates = categoria();
        Producto productoMate = producto(1L, "Mate", mates);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(productoMate));
        when(categoriaRepository.findByEmpresaIdOrderByNombreAsc(EMPRESA_ID)).thenReturn(List.of(mates));
        when(subcategoriaRepository.findByCategoriaIdOrderByNombreAsc(mates.getId())).thenReturn(List.of());
        when(tiendaCategoriaRepository.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of());

        List<PublicCategoriaMenuDto> resultado = publicTiendaService.listarCategoriasMenu(SLUG);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).imagenUrl()).isNull();
        assertThat(resultado.get(0).subcategorias()).isEmpty();
    }

    // --- listarAtributosFiltro ---

    @Test
    void listarAtributosFiltroDevuelveLosAtributosDeLaCategoriaConSusValoresAgrupados() {
        Empresa empresa = empresa();
        Categoria mates = categoria();

        AtributoFiltro material = new AtributoFiltro();
        material.setId(1L);
        material.setCategoriaId(mates.getId());
        material.setNombre("Material");

        AtributoFiltroValor acero = new AtributoFiltroValor();
        acero.setId(10L);
        acero.setAtributoFiltroId(material.getId());
        acero.setValor("Acero");

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(categoriaRepository.findByIdAndEmpresaId(mates.getId(), EMPRESA_ID)).thenReturn(Optional.of(mates));
        when(atributoFiltroRepository.findByCategoriaIdOrderByOrdenAsc(mates.getId())).thenReturn(List.of(material));
        when(atributoFiltroValorRepository.findByAtributoFiltroIdOrderByOrdenAsc(material.getId()))
                .thenReturn(List.of(acero));

        List<AtributoFiltroDto> resultado = publicTiendaService.listarAtributosFiltro(SLUG, mates.getId());

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Material");
        assertThat(resultado.get(0).valores()).hasSize(1);
        assertThat(resultado.get(0).valores().get(0).valor()).isEqualTo("Acero");
    }

    @Test
    void listarAtributosFiltroConCategoriaDeOtraEmpresaTiraCategoriaNoEncontrada() {
        Empresa empresa = empresa();

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(categoriaRepository.findByIdAndEmpresaId(999L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicTiendaService.listarAtributosFiltro(SLUG, 999L))
                .isInstanceOf(CategoriaNoEncontradaException.class);
    }

    // --- previewDescuentoCombo ---

    @Test
    void previewDescuentoComboSinDescuentoDevuelveElSingletonSinAplicar() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(50L, "Mate", categoria);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(50L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(reglaDescuentoComboRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of());
        when(calculadorDescuentoComboService.calcular(anyList(), anyList()))
                .thenReturn(ResultadoDescuentoCombo.sinDescuento());

        PreviewDescuentoComboRequest request = new PreviewDescuentoComboRequest(
                List.of(new PublicPedidoItemRequest(50L, 2, null, null, null, null)));

        PreviewDescuentoComboDto resultado = publicTiendaService.previewDescuentoCombo(SLUG, request);

        assertThat(resultado.aplica()).isFalse();
        assertThat(resultado.montoDescuento()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void previewDescuentoComboConDescuentoDevuelveElDetalleYElPorcentajeDeLaRegla() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(51L, "Mate", categoria);
        ReglaDescuentoCombo regla = new ReglaDescuentoCombo();
        regla.setPorcentaje(new BigDecimal("15"));

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(51L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(reglaDescuentoComboRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(regla));
        when(calculadorDescuentoComboService.calcular(anyList(), anyList()))
                .thenReturn(new ResultadoDescuentoCombo(regla, 1, new BigDecimal("150.00"), "Mates + Bombillas"));

        PreviewDescuentoComboRequest request = new PreviewDescuentoComboRequest(
                List.of(new PublicPedidoItemRequest(51L, 2, null, null, null, null)));

        PreviewDescuentoComboDto resultado = publicTiendaService.previewDescuentoCombo(SLUG, request);

        assertThat(resultado.aplica()).isTrue();
        assertThat(resultado.detalle()).isEqualTo("Mates + Bombillas");
        assertThat(resultado.porcentaje()).isEqualByComparingTo("15");
        assertThat(resultado.montoDescuento()).isEqualByComparingTo("150.00");
    }

    // --- crearPedido: variantes de color ---

    @Test
    void crearPedidoConVarianteElegidaYStockSuficienteAsociaColorAlItem() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(60L, "Mate imperial", categoria);
        ProductoVariante rojo = variante(producto, 100L, "Rojo");
        producto.getVariantes().add(rojo);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(60L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(stockDisponibleCalculator.calcularVariante(rojo)).thenReturn(5);
        when(clienteRepository.findByEmpresaIdAndTelefono(EMPRESA_ID, "1122334455"))
                .thenReturn(Optional.of(cliente(70L, "Fede", "1122334455")));
        when(calculadorDescuentoComboService.calcular(anyList(), anyList()))
                .thenReturn(ResultadoDescuentoCombo.sinDescuento());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicPedidoRequest request = new PublicPedidoRequest(
                "Fede", "1122334455",
                List.of(new PublicPedidoItemRequest(60L, 2, 100L, null, null, null)),
                null, null, null, null);

        publicTiendaService.crearPedido(SLUG, request);

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(captor.capture());
        assertThat(captor.getValue().getItems().get(0).getVarianteId()).isEqualTo(100L);
        assertThat(captor.getValue().getItems().get(0).getVarianteColor()).isEqualTo("Rojo");
    }

    @Test
    void crearPedidoConVarianteElegidaYStockInsuficienteLanzaExcepcionConColor() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(61L, "Mate imperial", categoria);
        ProductoVariante rojo = variante(producto, 101L, "Rojo");
        producto.getVariantes().add(rojo);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(61L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(stockDisponibleCalculator.calcularVariante(rojo)).thenReturn(1);
        when(clienteRepository.findByEmpresaIdAndTelefono(EMPRESA_ID, "1122334455"))
                .thenReturn(Optional.of(cliente(71L, "Fede", "1122334455")));

        PublicPedidoRequest request = new PublicPedidoRequest(
                "Fede", "1122334455",
                List.of(new PublicPedidoItemRequest(61L, 5, 101L, null, null, null)),
                null, null, null, null);

        assertThatThrownBy(() -> publicTiendaService.crearPedido(SLUG, request))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("Rojo");

        verify(ventaRepository, never()).save(any());
    }

    @Test
    void crearPedidoConVarianteIdQueNoPerteneceAlProductoLanzaProductoNoEncontrado() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(62L, "Mate imperial", categoria);
        producto.getVariantes().add(variante(producto, 102L, "Rojo"));

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(62L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(clienteRepository.findByEmpresaIdAndTelefono(EMPRESA_ID, "1122334455"))
                .thenReturn(Optional.of(cliente(72L, "Fede", "1122334455")));

        PublicPedidoRequest request = new PublicPedidoRequest(
                "Fede", "1122334455",
                List.of(new PublicPedidoItemRequest(62L, 1, 999L, null, null, null)),
                null, null, null, null);

        assertThatThrownBy(() -> publicTiendaService.crearPedido(SLUG, request))
                .isInstanceOf(ProductoNoEncontradoException.class);
    }

    // --- crearPedido: grabado personalizado ---

    @Test
    void crearPedidoConGrabadoSeleccionadoSumaElPrecioYArmaLaPersonalizacion() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(63L, "Mate imperial", categoria);
        producto.setPrecioVenta(new BigDecimal("1000.00"));
        producto.getGrabados().add(grabado(producto, 200L, "Virola", new BigDecimal("100.00")));

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(63L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(stockDisponibleCalculator.calcular(producto)).thenReturn(5);
        when(clienteRepository.findByEmpresaIdAndTelefono(EMPRESA_ID, "1122334455"))
                .thenReturn(Optional.of(cliente(73L, "Fede", "1122334455")));
        when(calculadorDescuentoComboService.calcular(anyList(), anyList()))
                .thenReturn(ResultadoDescuentoCombo.sinDescuento());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicPedidoRequest request = new PublicPedidoRequest(
                "Fede", "1122334455",
                List.of(new PublicPedidoItemRequest(63L, 1, null, List.of(200L), "Feliz cumple", null)),
                null, null, null, null);

        PublicPedidoResultadoDto resultado = publicTiendaService.crearPedido(SLUG, request);

        assertThat(resultado.total()).isEqualByComparingTo(new BigDecimal("1100.00"));

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(captor.capture());
        assertThat(captor.getValue().getItems().get(0).getPersonalizacion())
                .isEqualTo("Grabado en: Virola — Texto: Feliz cumple");
    }

    @Test
    void crearPedidoConGrabadoLugarIdQueNoPerteneceAlProductoLanzaProductoNoEncontrado() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(64L, "Mate imperial", categoria);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(64L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(stockDisponibleCalculator.calcular(producto)).thenReturn(5);
        when(clienteRepository.findByEmpresaIdAndTelefono(EMPRESA_ID, "1122334455"))
                .thenReturn(Optional.of(cliente(74L, "Fede", "1122334455")));

        PublicPedidoRequest request = new PublicPedidoRequest(
                "Fede", "1122334455",
                List.of(new PublicPedidoItemRequest(64L, 1, null, List.of(999L), null, null)),
                null, null, null, null);

        assertThatThrownBy(() -> publicTiendaService.crearPedido(SLUG, request))
                .isInstanceOf(ProductoNoEncontradoException.class);
    }

    @Test
    void crearPedidoConVariosGrabadosSumaTodosLosPreciosYOmiteElTextoCuandoNoVino() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(66L, "Mate imperial", categoria);
        producto.setPrecioVenta(new BigDecimal("1000.00"));
        producto.getGrabados().add(grabado(producto, 201L, "Virola", new BigDecimal("100.00")));
        producto.getGrabados().add(grabado(producto, 202L, "Cuerpo", new BigDecimal("250.00")));

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(66L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(stockDisponibleCalculator.calcular(producto)).thenReturn(5);
        when(clienteRepository.findByEmpresaIdAndTelefono(EMPRESA_ID, "1122334455"))
                .thenReturn(Optional.of(cliente(75L, "Fede", "1122334455")));
        when(calculadorDescuentoComboService.calcular(anyList(), anyList()))
                .thenReturn(ResultadoDescuentoCombo.sinDescuento());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicPedidoRequest request = new PublicPedidoRequest(
                "Fede", "1122334455",
                List.of(new PublicPedidoItemRequest(66L, 1, null, List.of(201L, 202L), null, null)),
                null, null, null, null);

        PublicPedidoResultadoDto resultado = publicTiendaService.crearPedido(SLUG, request);

        assertThat(resultado.total()).isEqualByComparingTo(new BigDecimal("1350.00"));

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(captor.capture());
        assertThat(captor.getValue().getItems().get(0).getPersonalizacion())
                .isEqualTo("Grabado en: Virola, Cuerpo");
    }

    // --- crearPedido: descuento combo, cupón y cliente nuevo ---

    @Test
    void crearPedidoConDescuentoComboAplicadoRestaElMontoDelTotal() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(65L, "Mate", categoria);
        producto.setPrecioVenta(new BigDecimal("1000.00"));

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(65L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(stockDisponibleCalculator.calcular(producto)).thenReturn(5);
        when(clienteRepository.findByEmpresaIdAndTelefono(EMPRESA_ID, "1122334455"))
                .thenReturn(Optional.of(cliente(75L, "Fede", "1122334455")));
        when(calculadorDescuentoComboService.calcular(anyList(), anyList()))
                .thenReturn(new ResultadoDescuentoCombo(null, 1, new BigDecimal("100.00"), "Mates + Bombillas"));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicPedidoRequest request = new PublicPedidoRequest(
                "Fede", "1122334455",
                List.of(new PublicPedidoItemRequest(65L, 1, null, null, null, null)),
                null, null, null, null);

        PublicPedidoResultadoDto resultado = publicTiendaService.crearPedido(SLUG, request);

        assertThat(resultado.total()).isEqualByComparingTo(new BigDecimal("900.00"));

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(captor.capture());
        assertThat(captor.getValue().getDescuentoComboMonto()).isEqualByComparingTo("100.00");
        assertThat(captor.getValue().getDescuentoComboDetalle()).isEqualTo("Mates + Bombillas");
    }

    @Test
    void crearPedidoConCuponCodigoCoincidenteYPorcentajeNuloLanzaCuponInvalido() {
        Empresa empresa = empresa();
        empresa.setTiendaCuponCodigo("PROMO10");
        empresa.setTiendaCuponPorcentaje(null);
        Categoria categoria = categoria();
        Producto producto = producto(66L, "Mate", categoria);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(66L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(stockDisponibleCalculator.calcular(producto)).thenReturn(5);
        when(clienteRepository.findByEmpresaIdAndTelefono(EMPRESA_ID, "1122334455"))
                .thenReturn(Optional.of(cliente(76L, "Fede", "1122334455")));
        when(calculadorDescuentoComboService.calcular(anyList(), anyList()))
                .thenReturn(ResultadoDescuentoCombo.sinDescuento());

        PublicPedidoRequest request = new PublicPedidoRequest(
                "Fede", "1122334455",
                List.of(new PublicPedidoItemRequest(66L, 1, null, null, null, null)),
                "PROMO10", null, null, null);

        assertThatThrownBy(() -> publicTiendaService.crearPedido(SLUG, request))
                .isInstanceOf(CuponInvalidoException.class);
    }

    @Test
    void crearPedidoConClienteNuevoLoCreaYLoAsociaALaVenta() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(67L, "Mate", categoria);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(67L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(stockDisponibleCalculator.calcular(producto)).thenReturn(5);
        when(clienteRepository.findByEmpresaIdAndTelefono(EMPRESA_ID, "9988776655")).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente nuevo = inv.getArgument(0);
            nuevo.setId(80L);
            return nuevo;
        });
        when(calculadorDescuentoComboService.calcular(anyList(), anyList()))
                .thenReturn(ResultadoDescuentoCombo.sinDescuento());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicPedidoRequest request = new PublicPedidoRequest(
                "Nueva Persona", "9988776655",
                List.of(new PublicPedidoItemRequest(67L, 1, null, null, null, null)),
                null, null, null, null);

        publicTiendaService.crearPedido(SLUG, request);

        ArgumentCaptor<Cliente> clienteCaptor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(clienteCaptor.capture());
        assertThat(clienteCaptor.getValue().getNombre()).isEqualTo("Nueva Persona");
        assertThat(clienteCaptor.getValue().getTelefono()).isEqualTo("9988776655");
    }

    @Test
    void crearPedidoConProductoInactivoLanzaProductoNoEncontrado() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(68L, "Mate", categoria);
        producto.setActivo(false);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByIdAndEmpresaId(68L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(clienteRepository.findByEmpresaIdAndTelefono(EMPRESA_ID, "1122334455"))
                .thenReturn(Optional.of(cliente(77L, "Fede", "1122334455")));

        PublicPedidoRequest request = new PublicPedidoRequest(
                "Fede", "1122334455",
                List.of(new PublicPedidoItemRequest(68L, 1, null, null, null, null)),
                null, null, null, null);

        assertThatThrownBy(() -> publicTiendaService.crearPedido(SLUG, request))
                .isInstanceOf(ProductoNoEncontradoException.class);
    }

    // --- listarProductos: subcategoría, reseña destacada y variantes ---

    @Test
    void listarProductosConSubcategoriaExponeSubcategoriaIdEnElDto() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Subcategoria sub = new Subcategoria();
        sub.setId(5L);
        sub.setCategoriaId(categoria.getId());
        sub.setNombre("Bombillas");
        Producto producto = producto(80L, "Bombilla premium", categoria);
        producto.setSubcategoria(sub);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));
        when(resenaRepository.findByEmpresaIdOrderByFechaDesc(EMPRESA_ID)).thenReturn(List.of());
        when(stockDisponibleCalculator.calcular(producto)).thenReturn(3);

        List<PublicProductoDto> resultado = publicTiendaService.listarProductos(SLUG);

        assertThat(resultado.get(0).subcategoriaId()).isEqualTo(5L);
    }

    @Test
    void listarProductosConResenaExponeResenaDestacada() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(81L, "Mate", categoria);
        Resena resena = new Resena();
        resena.setProductoId(81L);
        resena.setClienteNombre("Caro");
        resena.setComentario("Genial");
        resena.setFecha(LocalDateTime.now());

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));
        when(resenaRepository.findByEmpresaIdOrderByFechaDesc(EMPRESA_ID)).thenReturn(List.of(resena));
        when(stockDisponibleCalculator.calcular(producto)).thenReturn(3);

        List<PublicProductoDto> resultado = publicTiendaService.listarProductos(SLUG);

        assertThat(resultado.get(0).resenaDestacada()).isNotNull();
        assertThat(resultado.get(0).resenaDestacada().clienteNombre()).isEqualTo("Caro");
    }

    @Test
    void listarProductosConVariantesExponeElDisponiblePorColor() {
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(82L, "Mate", categoria);
        ProductoVariante rojo = variante(producto, 200L, "Rojo");
        producto.getVariantes().add(rojo);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));
        when(resenaRepository.findByEmpresaIdOrderByFechaDesc(EMPRESA_ID)).thenReturn(List.of());
        when(stockDisponibleCalculator.calcular(producto)).thenReturn(3);
        when(stockDisponibleCalculator.calcularVariante(rojo)).thenReturn(7);

        List<PublicProductoDto> resultado = publicTiendaService.listarProductos(SLUG);

        assertThat(resultado.get(0).variantes()).hasSize(1);
        assertThat(resultado.get(0).variantes().get(0).color()).isEqualTo("Rojo");
        assertThat(resultado.get(0).variantes().get(0).disponible()).isEqualTo(7);
    }

    // --- listarProductos: pool de fotos (fotoUrl derivado + fotos como lista) ---

    // Instancia propia con ProductoFotoResolver REAL (no el @Mock de la
    // clase, sin stubear): estos 3 tests ejercitan la regla de negocio de
    // verdad, no un valor mockeado — ver spec "Contrato público de fotos
    // como lista".
    private PublicTiendaServiceImpl servicioConResolverReal() {
        return new PublicTiendaServiceImpl(empresaRepository, productoRepository, clienteRepository, ventaRepository,
                ventaEstadoHistorialRepository, tiendaCategoriaRepository, categoriaRepository, subcategoriaRepository,
                atributoFiltroRepository, atributoFiltroValorRepository,
                tiendaBannerImagenRepository, resenaRepository, tiendaTipRepository,
                stockDisponibleCalculator, reglaDescuentoComboRepository, calculadorDescuentoComboService,
                imagenUploadValidator, new ProductoFotoResolver(), jwtService, passwordEncoder, googleTokenVerifier,
                tiendaBloqueRepository, tiendaBloqueCardRepository);
    }

    private ProductoFoto foto(Producto producto, Long id, Long varianteId, String url, int orden) {
        ProductoFoto foto = new ProductoFoto();
        foto.setId(id);
        foto.setProducto(producto);
        foto.setVarianteId(varianteId);
        foto.setUrl(url);
        foto.setOrden(orden);
        return foto;
    }

    @Test
    void listarProductosExponeFotoUrlDerivadaYLaListaCompletaDelPool() {
        PublicTiendaServiceImpl servicio = servicioConResolverReal();
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(90L, "Mate", categoria);
        producto.getFotos().add(foto(producto, 1L, 5L, "/con-color.jpg", 0));
        producto.getFotos().add(foto(producto, 2L, null, "/general.jpg", 1));

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));
        when(resenaRepository.findByEmpresaIdOrderByFechaDesc(EMPRESA_ID)).thenReturn(List.of());
        when(stockDisponibleCalculator.calcular(producto)).thenReturn(3);

        List<PublicProductoDto> resultado = servicio.listarProductos(SLUG);

        // Regla de miniatura: primera SIN color por orden -> "/general.jpg".
        assertThat(resultado.get(0).fotoUrl()).isEqualTo("/general.jpg");
        assertThat(resultado.get(0).fotos()).hasSize(2);
        assertThat(resultado.get(0).fotos().get(0).url()).isEqualTo("/con-color.jpg");
    }

    @Test
    void listarProductosConVarianteExponeSuFotoPropiaDelPoolComoFotoUrlDeLaVariante() {
        PublicTiendaServiceImpl servicio = servicioConResolverReal();
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(91L, "Mate", categoria);
        ProductoVariante rojo = variante(producto, 300L, "Rojo");
        producto.getVariantes().add(rojo);
        producto.getFotos().add(foto(producto, 3L, 300L, "/rojo.jpg", 0));

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));
        when(resenaRepository.findByEmpresaIdOrderByFechaDesc(EMPRESA_ID)).thenReturn(List.of());
        when(stockDisponibleCalculator.calcular(producto)).thenReturn(3);
        when(stockDisponibleCalculator.calcularVariante(rojo)).thenReturn(5);

        List<PublicProductoDto> resultado = servicio.listarProductos(SLUG);

        assertThat(resultado.get(0).variantes().get(0).fotoUrl()).isEqualTo("/rojo.jpg");
    }

    @Test
    void listarProductosSinFotosDejaFotoUrlNullYFotosVacia() {
        PublicTiendaServiceImpl servicio = servicioConResolverReal();
        Empresa empresa = empresa();
        Categoria categoria = categoria();
        Producto producto = producto(92L, "Mate", categoria);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));
        when(resenaRepository.findByEmpresaIdOrderByFechaDesc(EMPRESA_ID)).thenReturn(List.of());
        when(stockDisponibleCalculator.calcular(producto)).thenReturn(3);

        List<PublicProductoDto> resultado = servicio.listarProductos(SLUG);

        assertThat(resultado.get(0).fotoUrl()).isNull();
        assertThat(resultado.get(0).fotos()).isEmpty();
    }

    // --- obtenerEmpresa: banner imagenes ---

    @Test
    void obtenerEmpresaConBannerImagenesLasMapeaAlDtoPublico() {
        Empresa empresa = empresa();
        com.sistventas.backend.entity.TiendaBannerImagen heroImg = new com.sistventas.backend.entity.TiendaBannerImagen();
        heroImg.setImagenUrl("/uploads/empresas/hero1.jpg");
        heroImg.setProductoId(9L);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(tiendaBannerImagenRepository.findByEmpresaIdAndTipoOrderByOrden(EMPRESA_ID, "HERO")).thenReturn(List.of(heroImg));
        when(tiendaBannerImagenRepository.findByEmpresaIdAndTipoOrderByOrden(EMPRESA_ID, "VERTICAL")).thenReturn(List.of());

        PublicEmpresaDto resultado = publicTiendaService.obtenerEmpresa(SLUG);

        assertThat(resultado.bannerImagenes()).hasSize(1);
        assertThat(resultado.bannerImagenes().get(0).imagenUrl()).isEqualTo("/uploads/empresas/hero1.jpg");
        assertThat(resultado.bannerImagenes().get(0).productoId()).isEqualTo(9L);
    }

    private TiendaCategoria tiendaCategoria(Long id, Long categoriaId, String color, String imagenUrl) {
        TiendaCategoria tc = new TiendaCategoria();
        tc.setId(id);
        tc.setEmpresaId(EMPRESA_ID);
        tc.setCategoriaId(categoriaId);
        tc.setColor(color);
        tc.setImagenUrl(imagenUrl);
        return tc;
    }

    private ProductoGrabado grabado(Producto producto, Long id, String lugar, BigDecimal precio) {
        ProductoGrabado grabado = new ProductoGrabado();
        grabado.setId(id);
        grabado.setProducto(producto);
        grabado.setLugar(lugar);
        grabado.setPrecio(precio);
        return grabado;
    }

    // --- registrarCliente ---

    @Test
    void registrarClienteCreaUnClienteNuevoCuandoNoHayMatchPorEmail() {
        Empresa empresa = empresa();
        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(clienteRepository.findByEmpresaIdAndEmail(EMPRESA_ID, "nueva@x.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secreto1")).thenReturn("hash-secreto1");
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente guardado = inv.getArgument(0);
            guardado.setId(99L);
            return guardado;
        });
        when(jwtService.generateTokenCliente(any(Cliente.class))).thenReturn("jwt-de-prueba");

        RegistrarClienteRequest request = new RegistrarClienteRequest("Ana", "nueva@x.com", "secreto1", "1122334455");
        ClienteLoginResponse respuesta = publicTiendaService.registrarCliente(SLUG, request);

        assertThat(respuesta.cliente().id()).isEqualTo(99L);
        assertThat(respuesta.cliente().nombre()).isEqualTo("Ana");
        assertThat(respuesta.token()).isEqualTo("jwt-de-prueba");
        verify(clienteRepository).save(argThat(c ->
                c.getPasswordHash().equals("hash-secreto1") && c.getTelefono().equals("1122334455")));
    }

    // El teléfono ya NO se usa para buscar/fusionar con otra fila (ver
    // comentario en registrarCliente, 2026-09): dos personas que comparten
    // el mismo teléfono de prueba terminaban fusionadas en la misma cuenta,
    // la segunda heredando nombre/email/password de la primera. Este test
    // reemplaza al viejo "vincula con la compra de invitado por teléfono" —
    // ahora confirma exactamente lo contrario: crea una fila NUEVA aunque el
    // teléfono ya exista en otro Cliente (sea invitado o cuenta registrada).
    @Test
    void registrarClienteNoFusionaConOtroClienteQueComparteElMismoTelefono() {
        Empresa empresa = empresa();
        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(clienteRepository.findByEmpresaIdAndEmail(EMPRESA_ID, "ana@x.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secreto1")).thenReturn("hash-secreto1");
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente guardado = inv.getArgument(0);
            guardado.setId(99L);
            return guardado;
        });

        RegistrarClienteRequest request = new RegistrarClienteRequest("Ana", "ana@x.com", "secreto1", "1122334455");
        ClienteLoginResponse respuesta = publicTiendaService.registrarCliente(SLUG, request);

        assertThat(respuesta.cliente().id()).isEqualTo(99L);
        assertThat(respuesta.cliente().email()).isEqualTo("ana@x.com");
        verify(clienteRepository, never()).findByEmpresaIdAndTelefono(any(), any());
    }

    @Test
    void registrarClienteRechazaSiElEmailYaTieneContrasena() {
        Empresa empresa = empresa();
        Cliente existente = cliente(5L, "Otra", "000");
        existente.setEmail("ana@x.com");
        existente.setPasswordHash("ya-tiene-hash");
        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(clienteRepository.findByEmpresaIdAndEmail(EMPRESA_ID, "ana@x.com")).thenReturn(Optional.of(existente));

        RegistrarClienteRequest request = new RegistrarClienteRequest("Ana", "ana@x.com", "secreto1", "1122334455");

        assertThatThrownBy(() -> publicTiendaService.registrarCliente(SLUG, request))
                .isInstanceOf(ClienteYaRegistradoException.class);
        verify(clienteRepository, never()).save(any());
    }

    // Antes rechazaba el registro si el teléfono ya tenía una cuenta con
    // credenciales — el teléfono ya no es una clave de identidad (ver
    // comentario en registrarCliente, 2026-09), así que dos cuentas DISTINTAS
    // pueden compartir el mismo teléfono sin problema; el único bloqueo real
    // sigue siendo el email duplicado (ver
    // registrarClienteRechazaSiElEmailYaTieneContrasena).
    @Test
    void registrarClientePermiteRegistrarseAunqueOtraCuentaTengaElMismoTelefono() {
        Empresa empresa = empresa();
        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(clienteRepository.findByEmpresaIdAndEmail(EMPRESA_ID, "nueva@x.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secreto1")).thenReturn("hash-secreto1");
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente guardado = inv.getArgument(0);
            guardado.setId(77L);
            return guardado;
        });

        RegistrarClienteRequest request = new RegistrarClienteRequest("Ana", "nueva@x.com", "secreto1", "1122334455");
        ClienteLoginResponse respuesta = publicTiendaService.registrarCliente(SLUG, request);

        assertThat(respuesta.cliente().id()).isEqualTo(77L);
    }

    private Cliente cliente(Long id, String nombre, String telefono) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setEmpresaId(EMPRESA_ID);
        cliente.setNombre(nombre);
        cliente.setTelefono(telefono);
        cliente.setActivo(true);
        return cliente;
    }

    private ProductoVariante variante(Producto producto, Long id, String color) {
        ProductoVariante variante = new ProductoVariante();
        variante.setId(id);
        variante.setProducto(producto);
        variante.setColor(color);
        variante.setStock(10);
        return variante;
    }

    private Empresa empresa() {
        Empresa empresa = new Empresa();
        empresa.setId(EMPRESA_ID);
        empresa.setNombre("Mi Empresa");
        empresa.setSlug(SLUG);
        empresa.setTiendaHabilitada(true);
        return empresa;
    }

    private Categoria categoria() {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setEmpresaId(EMPRESA_ID);
        categoria.setNombre("Mates");
        return categoria;
    }

    private Producto producto(Long id, String nombre, Categoria categoria) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setEmpresaId(EMPRESA_ID);
        producto.setNombre(nombre);
        producto.setCategoria(categoria);
        producto.setPrecioVenta(new BigDecimal("100.00"));
        producto.setActivo(true);
        producto.setStock(10);
        return producto;
    }
}
