package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.CategoriaTiendaDto;
import com.sistventas.backend.dto.FotoUploadDto;
import com.sistventas.backend.dto.PreviewDescuentoComboDto;
import com.sistventas.backend.dto.PreviewDescuentoComboRequest;
import com.sistventas.backend.dto.PublicComponenteDto;
import com.sistventas.backend.dto.PublicEmpresaDto;
import com.sistventas.backend.dto.PublicPedidoEstadoDto;
import com.sistventas.backend.dto.PublicPedidoItemRequest;
import com.sistventas.backend.dto.PublicPedidoRequest;
import com.sistventas.backend.dto.PublicPedidoResultadoDto;
import com.sistventas.backend.dto.PublicProductoDto;
import com.sistventas.backend.dto.PublicTestimonioDto;
import com.sistventas.backend.dto.PublicTipDto;
import com.sistventas.backend.entity.Categoria;
import com.sistventas.backend.entity.CanalTestimonio;
import com.sistventas.backend.entity.Cliente;
import com.sistventas.backend.entity.Empresa;
import com.sistventas.backend.entity.EstadoVenta;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ProductoComponente;
import com.sistventas.backend.entity.ProductoGrabado;
import com.sistventas.backend.entity.ProductoVariante;
import com.sistventas.backend.entity.ReglaDescuentoCombo;
import com.sistventas.backend.entity.Resena;
import com.sistventas.backend.entity.Subcategoria;
import com.sistventas.backend.entity.TiendaCategoria;
import com.sistventas.backend.entity.TiendaTestimonio;
import com.sistventas.backend.entity.TiendaTip;
import com.sistventas.backend.entity.Venta;
import com.sistventas.backend.dto.ResenaDto;
import com.sistventas.backend.exception.AccionNoPermitidaException;
import com.sistventas.backend.exception.CuponInvalidoException;
import com.sistventas.backend.exception.ProductoNoEncontradoException;
import com.sistventas.backend.exception.StockInsuficienteException;
import com.sistventas.backend.exception.TiendaNoEncontradaException;
import com.sistventas.backend.exception.VentaNoEncontradaException;
import com.sistventas.backend.repository.CategoriaRepository;
import com.sistventas.backend.repository.ClienteRepository;
import com.sistventas.backend.repository.EmpresaRepository;
import com.sistventas.backend.repository.ProductoRepository;
import com.sistventas.backend.repository.ReglaDescuentoComboRepository;
import com.sistventas.backend.repository.ResenaRepository;
import com.sistventas.backend.repository.TiendaBannerImagenRepository;
import com.sistventas.backend.repository.TiendaCategoriaRepository;
import com.sistventas.backend.repository.TiendaTestimonioRepository;
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
    private TiendaBannerImagenRepository tiendaBannerImagenRepository;

    @Mock
    private ResenaRepository resenaRepository;

    @Mock
    private TiendaTestimonioRepository tiendaTestimonioRepository;

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

    @InjectMocks
    private PublicTiendaServiceImpl publicTiendaService;

    @Test
    void obtenerEmpresaExponeTiendaFuenteYTiendaTemaDeLaEmpresa() {
        Empresa empresa = empresa();
        empresa.setTiendaFuente("moderna");
        empresa.setTiendaTema("marino-dorado");
        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(tiendaBannerImagenRepository.findByEmpresaIdAndTipoOrderByOrden(EMPRESA_ID, "HERO")).thenReturn(List.of());
        when(tiendaBannerImagenRepository.findByEmpresaIdAndTipoOrderByOrden(EMPRESA_ID, "VERTICAL")).thenReturn(List.of());

        PublicEmpresaDto resultado = publicTiendaService.obtenerEmpresa(SLUG);

        assertThat(resultado.tiendaFuente()).isEqualTo("moderna");
        assertThat(resultado.tiendaTema()).isEqualTo("marino-dorado");
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

    // --- listarTestimonios / listarTips / listarResenas: scoping por empresa ---

    @Test
    void listarTestimoniosTraeSoloLosMapeadosAlDtoPublicoSinExponerIdNiOrden() {
        Empresa empresa = empresa();
        TiendaTestimonio testimonio = new TiendaTestimonio();
        testimonio.setEmpresaId(EMPRESA_ID);
        testimonio.setClienteNombre("Juan");
        testimonio.setComentario("Excelente atención");
        testimonio.setCanal(CanalTestimonio.WHATSAPP);

        when(empresaRepository.findBySlugAndTiendaHabilitadaTrue(SLUG)).thenReturn(Optional.of(empresa));
        when(tiendaTestimonioRepository.findByEmpresaIdOrderByOrdenAscIdAsc(EMPRESA_ID)).thenReturn(List.of(testimonio));

        List<PublicTestimonioDto> resultado = publicTiendaService.listarTestimonios(SLUG);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).clienteNombre()).isEqualTo("Juan");
        assertThat(resultado.get(0).comentario()).isEqualTo("Excelente atención");
        assertThat(resultado.get(0).canal()).isEqualTo(CanalTestimonio.WHATSAPP);
        verify(tiendaTestimonioRepository).findByEmpresaIdOrderByOrdenAscIdAsc(EMPRESA_ID);
    }

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
