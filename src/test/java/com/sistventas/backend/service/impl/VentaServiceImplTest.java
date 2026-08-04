package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarEstadoVentaRequest;
import com.sistventas.backend.dto.FotoUploadDto;
import com.sistventas.backend.dto.TextoCompartirDto;
import com.sistventas.backend.dto.VentaDto;
import com.sistventas.backend.dto.VentaEstadoHistorialDto;
import com.sistventas.backend.dto.VentaItemRequest;
import com.sistventas.backend.dto.VentaRequest;
import com.sistventas.backend.entity.Cliente;
import com.sistventas.backend.entity.EstadoVenta;
import com.sistventas.backend.entity.Insumo;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ProductoComponente;
import com.sistventas.backend.entity.ProductoInsumo;
import com.sistventas.backend.entity.ProductoVariante;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.Usuario;
import com.sistventas.backend.entity.Venta;
import com.sistventas.backend.entity.VentaEstadoHistorial;
import com.sistventas.backend.entity.VentaItem;
import com.sistventas.backend.exception.ClienteNoEncontradoException;
import com.sistventas.backend.exception.ProductoNoEncontradoException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.exception.StockInsuficienteException;
import com.sistventas.backend.exception.VentaNoEncontradaException;
import com.sistventas.backend.repository.ClienteRepository;
import com.sistventas.backend.repository.InsumoRepository;
import com.sistventas.backend.repository.ProductoRepository;
import com.sistventas.backend.repository.ProductoVarianteRepository;
import com.sistventas.backend.repository.UsuarioRepository;
import com.sistventas.backend.repository.VentaEstadoHistorialRepository;
import com.sistventas.backend.repository.VentaRepository;
import com.sistventas.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests de la lógica de negocio de VentaServiceImpl: cálculo de
// totales/precios, el hecho de que un PRESUPUESTO no reserva stock, y el
// movimiento de stock (descontar/restaurar) al cambiar de estado — incluida
// la variante con kits y con productos de color. Los repositorios se mockean
// enteros; las strategies de consumo de insumos (ConsumoDirectoStrategy /
// ConsumoEnComboStrategy) se usan reales porque no tienen dependencias y son
// las que definen el comportamiento que se está probando.
@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private InsumoRepository insumoRepository;
    @Mock
    private ProductoVarianteRepository productoVarianteRepository;
    @Mock
    private VentaEstadoHistorialRepository ventaEstadoHistorialRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private VentaServiceImpl ventaService;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @BeforeEach
    void setUp() {
        ventaService = new VentaServiceImpl(
                ventaRepository,
                clienteRepository,
                productoRepository,
                insumoRepository,
                productoVarianteRepository,
                ventaEstadoHistorialRepository,
                usuarioRepository,
                new ImagenUploadValidator(),
                new ConsumoDirectoStrategy(),
                new ConsumoEnComboStrategy());

        // lenient: varios tests de camino de error nunca llegan a guardar,
        // y no interesa forzarlos a stubear esto explícitamente.
        lenient().when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- crear(): cálculo de precios y totales ---

    @Test
    void crearCalculaSubtotalYTotalSumandoLosItems() {
        Producto mate = producto(1L, "Mate", 10, "100.00");
        Producto bombilla = producto(2L, "Bombilla", 10, "50.00");
        mockearClienteYProductos(mate, bombilla);

        VentaRequest request = ventaRequest(BigDecimal.ZERO,
                itemRequest(1L, 2, null, null),
                itemRequest(2L, 1, null, null));

        VentaDto resultado = ventaService.crear(request, principal);

        assertThat(resultado.subtotal()).isEqualByComparingTo("250.00");
        assertThat(resultado.total()).isEqualByComparingTo("250.00");
    }

    @Test
    void crearAplicaDescuentoPorcentajeSobreElSubtotal() {
        Producto mate = producto(1L, "Mate", 10, "100.00");
        mockearClienteYProductos(mate);

        VentaRequest request = ventaRequest(new BigDecimal("10"), itemRequest(1L, 2, null, null));

        VentaDto resultado = ventaService.crear(request, principal);

        assertThat(resultado.subtotal()).isEqualByComparingTo("200.00");
        assertThat(resultado.total()).isEqualByComparingTo("180.00");
    }

    @Test
    void crearUsaPrecioPorMayorCuandoLaCantidadAlcanzaElMinimo() {
        Producto producto = producto(1L, "Mate", 10, "100.00");
        producto.setPrecioPorMayor(new BigDecimal("80.00"));
        producto.setCantidadMinimaMayorista(5);
        mockearClienteYProductos(producto);

        VentaRequest request = ventaRequest(BigDecimal.ZERO, itemRequest(1L, 5, null, null));

        VentaDto resultado = ventaService.crear(request, principal);

        assertThat(resultado.items().get(0).precioUnitario()).isEqualByComparingTo("80.00");
        assertThat(resultado.total()).isEqualByComparingTo("400.00");
    }

    @Test
    void crearUsaPrecioNormalCuandoNoLlegaAlMinimoMayorista() {
        Producto producto = producto(1L, "Mate", 10, "100.00");
        producto.setPrecioPorMayor(new BigDecimal("80.00"));
        producto.setCantidadMinimaMayorista(5);
        mockearClienteYProductos(producto);

        VentaRequest request = ventaRequest(BigDecimal.ZERO, itemRequest(1L, 4, null, null));

        VentaDto resultado = ventaService.crear(request, principal);

        assertThat(resultado.items().get(0).precioUnitario()).isEqualByComparingTo("100.00");
    }

    @Test
    void crearRespetaElPrecioUnitarioExplicitoDelRequestPorEncimaDelPrecioDelProducto() {
        Producto producto = producto(1L, "Mate", 10, "100.00");
        mockearClienteYProductos(producto);

        VentaRequest request = ventaRequest(BigDecimal.ZERO, itemRequest(1L, 1, new BigDecimal("42.00"), null));

        VentaDto resultado = ventaService.crear(request, principal);

        assertThat(resultado.items().get(0).precioUnitario()).isEqualByComparingTo("42.00");
        assertThat(resultado.total()).isEqualByComparingTo("42.00");
    }

    // --- crear(): un presupuesto no reserva stock ---

    @Test
    void crearNuevaVentaQuedaEnPresupuestoYNoValidaStockDisponible() {
        // Regla de negocio explícita del service (ver holdsStock): un
        // PRESUPUESTO nunca reserva stock, así que pedir más de lo
        // disponible al CREAR no debe explotar — recién se valida al
        // confirmar (actualizarEstado).
        Producto producto = producto(1L, "Mate", 1, "100.00");
        mockearClienteYProductos(producto);

        VentaRequest request = ventaRequest(BigDecimal.ZERO, itemRequest(1L, 999, null, null));

        VentaDto resultado = ventaService.crear(request, principal);

        assertThat(resultado.estado()).isEqualTo(EstadoVenta.PRESUPUESTO);
        verify(productoRepository, never()).save(any());
    }

    // --- crear(): casos de error ---

    @Test
    void crearConProductoInexistenteLanzaExcepcion() {
        when(clienteRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(cliente(1L, "Ana")));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.empty());

        VentaRequest request = ventaRequest(BigDecimal.ZERO, itemRequest(1L, 1, null, null));

        assertThatThrownBy(() -> ventaService.crear(request, principal))
                .isInstanceOf(ProductoNoEncontradoException.class);
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void crearConProductoInactivoLanzaExcepcion() {
        Producto producto = producto(1L, "Mate", 10, "100.00");
        producto.setActivo(false);
        when(clienteRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(cliente(1L, "Ana")));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        VentaRequest request = ventaRequest(BigDecimal.ZERO, itemRequest(1L, 1, null, null));

        assertThatThrownBy(() -> ventaService.crear(request, principal))
                .isInstanceOf(ProductoNoEncontradoException.class);
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void crearConClienteInexistenteLanzaExcepcion() {
        when(clienteRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.empty());

        VentaRequest request = ventaRequest(BigDecimal.ZERO, itemRequest(1L, 1, null, null));

        assertThatThrownBy(() -> ventaService.crear(request, principal))
                .isInstanceOf(ClienteNoEncontradoException.class);
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void crearSinEmpresaEnElPrincipalLanzaExcepcion() {
        UserPrincipal superAdmin = new UserPrincipal(1L, null, true, null);
        VentaRequest request = ventaRequest(BigDecimal.ZERO, itemRequest(1L, 1, null, null));

        assertThatThrownBy(() -> ventaService.crear(request, superAdmin))
                .isInstanceOf(SinEmpresaException.class);
        verify(clienteRepository, never()).findByIdAndEmpresaId(any(), any());
    }

    // --- actualizarEstado(): venta no encontrada (scoping multi-tenant) ---

    @Test
    void actualizarEstadoDeVentaInexistenteLanzaExcepcion() {
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ventaService.actualizarEstado(5L, new ActualizarEstadoVentaRequest(EstadoVenta.CONFIRMADA), principal))
                .isInstanceOf(VentaNoEncontradaException.class);
    }

    // --- actualizarEstado(): descuento de stock al confirmar ---

    @Test
    void actualizarEstadoDePresupuestoAConfirmadaDescuentaStock() {
        Producto producto = producto(1L, "Mate", 10, "100.00");
        Venta venta = venta(5L, EstadoVenta.PRESUPUESTO, ventaItem(1L, 3, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        VentaDto resultado = ventaService.actualizarEstado(5L, new ActualizarEstadoVentaRequest(EstadoVenta.CONFIRMADA), principal);

        assertThat(resultado.estado()).isEqualTo(EstadoVenta.CONFIRMADA);
        assertThat(producto.getStock()).isEqualTo(7);
        verify(productoRepository).save(producto);
    }

    @Test
    void actualizarEstadoConStockInsuficienteLanzaExcepcionYNoPersisteNada() {
        Producto producto = producto(1L, "Mate", 1, "100.00");
        Venta venta = venta(5L, EstadoVenta.PRESUPUESTO, ventaItem(1L, 5, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> ventaService.actualizarEstado(5L, new ActualizarEstadoVentaRequest(EstadoVenta.CONFIRMADA), principal))
                .isInstanceOf(StockInsuficienteException.class);

        assertThat(producto.getStock()).isEqualTo(1);
        verify(productoRepository, never()).save(any());
        verify(ventaRepository, never()).save(any());
        verify(ventaEstadoHistorialRepository, never()).save(any());
    }

    @Test
    void descontarStockAgrupaCantidadesDelMismoProductoAntesDeValidar() {
        // Dos líneas de 3 c/u del mismo producto individualmente entrarían
        // en un stock de 5, pero la demanda COMBINADA (6) no alcanza: el
        // service tiene que agrupar antes de comparar contra stock.
        Producto producto = producto(1L, "Mate", 5, "100.00");
        Venta venta = venta(5L, EstadoVenta.PRESUPUESTO, ventaItem(1L, 3, null), ventaItem(1L, 3, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> ventaService.actualizarEstado(5L, new ActualizarEstadoVentaRequest(EstadoVenta.CONFIRMADA), principal))
                .isInstanceOf(StockInsuficienteException.class);
    }

    @Test
    void actualizarEstadoEntreEstadosConStockYaReservadoNoVuelveADescontar() {
        // CONFIRMADA -> EN_PROCESO: ambos estados ya tienen stock
        // reservado, así que no debe tocarlo de nuevo (se descontó una sola
        // vez al confirmar).
        Venta venta = venta(5L, EstadoVenta.CONFIRMADA, ventaItem(1L, 3, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));

        VentaDto resultado = ventaService.actualizarEstado(5L, new ActualizarEstadoVentaRequest(EstadoVenta.EN_PROCESO), principal);

        assertThat(resultado.estado()).isEqualTo(EstadoVenta.EN_PROCESO);
        verify(productoRepository, never()).findByIdAndEmpresaId(any(), any());
        verify(ventaEstadoHistorialRepository).save(any());
    }

    // --- actualizarEstado(): cancelar restaura stock ---

    @Test
    void actualizarEstadoACanceladaRestauraStockReservado() {
        Producto producto = producto(1L, "Mate", 3, "100.00");
        Venta venta = venta(5L, EstadoVenta.CONFIRMADA, ventaItem(1L, 4, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        VentaDto resultado = ventaService.actualizarEstado(5L, new ActualizarEstadoVentaRequest(EstadoVenta.CANCELADA), principal);

        assertThat(resultado.estado()).isEqualTo(EstadoVenta.CANCELADA);
        assertThat(producto.getStock()).isEqualTo(7);
        verify(productoRepository).save(producto);
    }

    @Test
    void actualizarEstadoACanceladaDesdePresupuestoNoRestauraStock() {
        // PRESUPUESTO nunca reservó stock, así que cancelarlo no debe
        // tocarlo (ni siquiera intentar resolver el producto).
        Venta venta = venta(5L, EstadoVenta.PRESUPUESTO, ventaItem(1L, 4, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));

        VentaDto resultado = ventaService.actualizarEstado(5L, new ActualizarEstadoVentaRequest(EstadoVenta.CANCELADA), principal);

        assertThat(resultado.estado()).isEqualTo(EstadoVenta.CANCELADA);
        verify(productoRepository, never()).findByIdAndEmpresaId(any(), any());
    }

    @Test
    void actualizarEstadoSinCambioRealNoRegistraHistorial() {
        // Cancelar una venta que YA está cancelada no debe generar una fila
        // nueva en el historial de auditoría (ver registrarCambioEstado).
        Venta venta = venta(5L, EstadoVenta.CANCELADA, ventaItem(1L, 4, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));

        ventaService.actualizarEstado(5L, new ActualizarEstadoVentaRequest(EstadoVenta.CANCELADA), principal);

        verify(ventaEstadoHistorialRepository, never()).save(any());
        verify(ventaRepository).save(venta);
    }

    // --- eliminar(): es un alias de cancelar ---

    @Test
    void eliminarCancelaYRestauraStockReservado() {
        Producto producto = producto(1L, "Mate", 5, "100.00");
        Venta venta = venta(5L, EstadoVenta.CONFIRMADA, ventaItem(1L, 2, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        ventaService.eliminar(5L, principal);

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CANCELADA);
        assertThat(producto.getStock()).isEqualTo(7);
        verify(ventaRepository).save(venta);
        verify(ventaEstadoHistorialRepository).save(any());
    }

    // --- actualizar(): edición de una venta con stock reservado ---

    @Test
    void actualizarVentaConStockReservadoRestauraElStockViejoYDescuentaElNuevo() {
        // La venta ya tenía 2 unidades reservadas (por eso el stock actual
        // del producto es 3, no 5): editar a 4 unidades tiene que devolver
        // las 2 viejas (stock vuelve a 5) y descontar las 4 nuevas (stock
        // termina en 1).
        Producto producto = producto(1L, "Mate", 3, "100.00");
        Venta existente = venta(5L, EstadoVenta.CONFIRMADA, ventaItem(1L, 2, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(clienteRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(cliente(1L, "Ana")));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        VentaRequest request = ventaRequest(BigDecimal.ZERO, itemRequest(1L, 4, null, null));

        ventaService.actualizar(5L, request, principal);

        assertThat(producto.getStock()).isEqualTo(1);
    }

    @Test
    void actualizarVentaEnPresupuestoNoTocaStock() {
        Producto producto = producto(1L, "Mate", 3, "100.00");
        Venta existente = venta(5L, EstadoVenta.PRESUPUESTO, ventaItem(1L, 2, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(clienteRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(cliente(1L, "Ana")));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        VentaRequest request = ventaRequest(BigDecimal.ZERO, itemRequest(1L, 999, null, null));

        ventaService.actualizar(5L, request, principal);

        assertThat(producto.getStock()).isEqualTo(3);
    }

    // --- kits: la demanda cae sobre los componentes, no sobre el kit ---

    @Test
    void confirmarVentaConKitDescuentaStockDeLosComponentesNoDelKit() {
        Producto mate = producto(2L, "Mate", 5, "50.00");
        Producto termo = producto(3L, "Termo", 2, "150.00");
        Producto kit = producto(1L, "Combo Mate + Termo", 0, "180.00");
        kit.getComponentes().add(componente(kit, mate, 1));
        kit.getComponentes().add(componente(kit, termo, 1));

        Venta venta = venta(5L, EstadoVenta.PRESUPUESTO, ventaItem(1L, 2, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(kit));

        ventaService.actualizarEstado(5L, new ActualizarEstadoVentaRequest(EstadoVenta.CONFIRMADA), principal);

        assertThat(mate.getStock()).isEqualTo(3);
        assertThat(termo.getStock()).isEqualTo(0);
        assertThat(kit.getStock()).isEqualTo(0);
    }

    // --- variantes de color: la demanda cae sobre la variante, no sobre Producto.stock ---

    @Test
    void confirmarVentaConVarianteDescuentaStockDeLaVarianteNoDelProductoBase() {
        Producto remera = producto(1L, "Remera", 0, "100.00");
        ProductoVariante rojo = variante(100L, remera, "Rojo", 5);
        ProductoVariante azul = variante(101L, remera, "Azul", 3);
        remera.getVariantes().add(rojo);
        remera.getVariantes().add(azul);

        Venta venta = venta(5L, EstadoVenta.PRESUPUESTO, ventaItem(1L, 2, 100L));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(remera));

        ventaService.actualizarEstado(5L, new ActualizarEstadoVentaRequest(EstadoVenta.CONFIRMADA), principal);

        assertThat(rojo.getStock()).isEqualTo(3);
        assertThat(azul.getStock()).isEqualTo(3);
        assertThat(remera.getStock()).isEqualTo(0);
        verify(productoVarianteRepository).save(rojo);
    }

    @Test
    void confirmarVentaConVarianteIdInexistenteLanzaExcepcionReciénAlDescontarStock() {
        // crear()/actualizar() no validan que el varianteId pertenezca al
        // producto (ver aplicarDatos): la referencia recién se resuelve acá,
        // al mover stock.
        Producto remera = producto(1L, "Remera", 0, "100.00");
        remera.getVariantes().add(variante(100L, remera, "Rojo", 5));

        Venta venta = venta(5L, EstadoVenta.PRESUPUESTO, ventaItem(1L, 1, 999L));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(remera));

        assertThatThrownBy(() -> ventaService.actualizarEstado(5L, new ActualizarEstadoVentaRequest(EstadoVenta.CONFIRMADA), principal))
                .isInstanceOf(ProductoNoEncontradoException.class);
    }

    // --- Fix: crear()/actualizar() completan el snapshot de color de la
    // variante, igual que PublicTiendaServiceImpl.crearPedido ---

    @Test
    void crearConVarianteDejaSnapshotDeColorEnElItem() {
        // Antes, VentaServiceImpl.aplicarDatos solo copiaba varianteId y
        // dejaba varianteColor en null (a diferencia de
        // PublicTiendaServiceImpl.crearPedido, que sí lo snapshoteaba) — una
        // venta cargada desde el panel admin con variante de color perdía el
        // color en el DTO y en el texto de WhatsApp, aunque el stock de la
        // variante se moviera bien igual.
        Producto remera = producto(1L, "Remera", 0, "100.00");
        remera.getVariantes().add(variante(100L, remera, "Rojo", 5));
        mockearClienteYProductos(remera);

        VentaRequest request = ventaRequest(BigDecimal.ZERO, itemRequest(1L, 1, null, 100L));

        VentaDto resultado = ventaService.crear(request, principal);

        assertThat(resultado.items().get(0).varianteId()).isEqualTo(100L);
        assertThat(resultado.items().get(0).varianteColor()).isEqualTo("Rojo");
    }

    @Test
    void crearConVarianteIdQueNoExisteEnElProductoDejaColorNulo() {
        // Defensivo: si el varianteId no matchea ninguna variante real del
        // producto (borrada, o de otro producto), no explota — el snapshot
        // de color simplemente queda sin completar, igual que si no hubiera
        // variante. La validación de que el varianteId sea válido es
        // responsabilidad de otro punto del flujo (mover stock).
        Producto remera = producto(1L, "Remera", 10, "100.00");
        mockearClienteYProductos(remera);

        VentaRequest request = ventaRequest(BigDecimal.ZERO, itemRequest(1L, 1, null, 999L));

        VentaDto resultado = ventaService.crear(request, principal);

        assertThat(resultado.items().get(0).varianteId()).isEqualTo(999L);
        assertThat(resultado.items().get(0).varianteColor()).isNull();
    }

    // --- crear(): descuento nulo usa cero por defecto ---

    @Test
    void crearConDescuentoPorcentajeNuloUsaCeroPorDefecto() {
        Producto producto = producto(1L, "Mate", 10, "100.00");
        mockearClienteYProductos(producto);

        VentaRequest request = new VentaRequest(1L, null, null, null, List.of(itemRequest(1L, 2, null, null)));

        VentaDto resultado = ventaService.crear(request, principal);

        assertThat(resultado.descuentoPorcentaje()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.total()).isEqualByComparingTo("200.00");
    }

    // --- listar() ---

    @Test
    void listarPorClienteIncluyeTodasLasVentasIncluidasLasCanceladas() {
        Venta venta = venta(5L, EstadoVenta.CANCELADA, ventaItem(1L, 1, null));
        when(ventaRepository.findByEmpresaIdAndClienteIdOrderByFechaPedidoDesc(EMPRESA_ID, 1L))
                .thenReturn(List.of(venta));
        when(clienteRepository.findAllById(any())).thenReturn(List.of(cliente(1L, "Ana")));

        List<VentaDto> resultado = ventaService.listar(null, 1L, principal);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).clienteNombre()).isEqualTo("Ana");
        assertThat(resultado.get(0).estado()).isEqualTo(EstadoVenta.CANCELADA);
    }

    @Test
    void listarPorEstadoFiltraPorEseEstado() {
        Venta venta = venta(5L, EstadoVenta.CONFIRMADA, ventaItem(1L, 1, null));
        when(ventaRepository.findByEmpresaIdAndEstado(EMPRESA_ID, EstadoVenta.CONFIRMADA)).thenReturn(List.of(venta));
        when(clienteRepository.findAllById(any())).thenReturn(List.of(cliente(1L, "Ana")));

        List<VentaDto> resultado = ventaService.listar(EstadoVenta.CONFIRMADA, null, principal);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).estado()).isEqualTo(EstadoVenta.CONFIRMADA);
    }

    @Test
    void listarSinFiltrosExcluyeLasCanceladasYOrdenaConLasSinFechaAlFinal() {
        Venta conFecha = venta(1L, EstadoVenta.PRESUPUESTO, ventaItem(1L, 1, null));
        conFecha.setFechaEntrega(LocalDate.now().plusDays(1));
        Venta sinFecha = venta(2L, EstadoVenta.PRESUPUESTO, ventaItem(1L, 1, null));
        when(ventaRepository.findByEmpresaIdAndEstadoNot(EMPRESA_ID, EstadoVenta.CANCELADA))
                .thenReturn(List.of(sinFecha, conFecha));
        when(clienteRepository.findAllById(any())).thenReturn(List.of(cliente(1L, "Ana")));

        List<VentaDto> resultado = ventaService.listar(null, null, principal);

        assertThat(resultado).extracting(VentaDto::id).containsExactly(1L, 2L);
    }

    // --- historialEstados() ---

    @Test
    void historialEstadosMapeaCadaFilaIncluyendoElNombreDelUsuario() {
        Venta venta = venta(5L, EstadoVenta.CONFIRMADA, ventaItem(1L, 1, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        VentaEstadoHistorial historial = new VentaEstadoHistorial();
        historial.setId(1L);
        historial.setEstadoAnterior(EstadoVenta.PRESUPUESTO);
        historial.setEstadoNuevo(EstadoVenta.CONFIRMADA);
        historial.setUsuarioId(99L);
        historial.setFecha(LocalDateTime.now());
        when(ventaEstadoHistorialRepository.findByVentaIdOrderByFechaDescIdDesc(5L)).thenReturn(List.of(historial));
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setNombre("Lucas");
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(usuario));

        List<VentaEstadoHistorialDto> resultado = ventaService.historialEstados(5L, principal);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).usuarioNombre()).isEqualTo("Lucas");
    }

    @Test
    void historialEstadosConUsuarioBorradoMuestraUsuarioEliminado() {
        Venta venta = venta(5L, EstadoVenta.CONFIRMADA, ventaItem(1L, 1, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        VentaEstadoHistorial historial = new VentaEstadoHistorial();
        historial.setId(1L);
        historial.setEstadoAnterior(EstadoVenta.PRESUPUESTO);
        historial.setEstadoNuevo(EstadoVenta.CONFIRMADA);
        historial.setUsuarioId(404L);
        historial.setFecha(LocalDateTime.now());
        when(ventaEstadoHistorialRepository.findByVentaIdOrderByFechaDescIdDesc(5L)).thenReturn(List.of(historial));
        when(usuarioRepository.findById(404L)).thenReturn(Optional.empty());

        List<VentaEstadoHistorialDto> resultado = ventaService.historialEstados(5L, principal);

        assertThat(resultado.get(0).usuarioNombre()).isEqualTo("Usuario eliminado");
    }

    // --- obtener() ---

    @Test
    void obtenerDevuelveLaVentaConNombreDeCliente() {
        Venta venta = venta(5L, EstadoVenta.PRESUPUESTO, ventaItem(1L, 1, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(clienteRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(cliente(1L, "Ana")));

        VentaDto resultado = ventaService.obtener(5L, principal);

        assertThat(resultado.clienteNombre()).isEqualTo("Ana");
    }

    // --- restaurar stock de variantes al cancelar ---

    @Test
    void cancelarVentaConVarianteRestauraStockDeLaVariante() {
        Producto remera = producto(1L, "Remera", 0, "100.00");
        ProductoVariante rojo = variante(100L, remera, "Rojo", 3);
        remera.getVariantes().add(rojo);
        Venta venta = venta(5L, EstadoVenta.CONFIRMADA, ventaItem(1L, 2, 100L));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(remera));

        ventaService.actualizarEstado(5L, new ActualizarEstadoVentaRequest(EstadoVenta.CANCELADA), principal);

        assertThat(rojo.getStock()).isEqualTo(5);
        verify(productoVarianteRepository).save(rojo);
    }

    // --- validarStockSuficiente: variante con stock insuficiente ---

    @Test
    void confirmarVentaConStockDeVarianteInsuficienteLanzaExcepcionConElColorEnElMensaje() {
        Producto remera = producto(1L, "Remera", 0, "100.00");
        ProductoVariante rojo = variante(100L, remera, "Rojo", 1);
        remera.getVariantes().add(rojo);
        Venta venta = venta(5L, EstadoVenta.PRESUPUESTO, ventaItem(1L, 5, 100L));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(remera));

        assertThatThrownBy(() -> ventaService.actualizarEstado(5L, new ActualizarEstadoVentaRequest(EstadoVenta.CONFIRMADA), principal))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("Rojo");
    }

    // --- productos con receta: consumo de insumos ---

    @Test
    void confirmarVentaDeProductoConRecetaDescuentaStockDeCadaInsumo() {
        Insumo hilo = insumo(1L, "Hilo", "50");
        Producto gorro = producto(1L, "Gorro tejido", 0, "300.00");
        gorro.getInsumos().add(productoInsumo(gorro, hilo, "2"));
        Venta venta = venta(5L, EstadoVenta.PRESUPUESTO, ventaItem(1L, 3, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(gorro));
        when(insumoRepository.findAllById(Set.of(1L))).thenReturn(List.of(hilo));

        ventaService.actualizarEstado(5L, new ActualizarEstadoVentaRequest(EstadoVenta.CONFIRMADA), principal);

        assertThat(hilo.getStock()).isEqualByComparingTo("44");
        verify(insumoRepository).save(hilo);
    }

    @Test
    void confirmarVentaConStockDeInsumoInsuficienteLanzaExcepcionYNoDescuentaNada() {
        Insumo hilo = insumo(1L, "Hilo", "5");
        Producto gorro = producto(1L, "Gorro tejido", 0, "300.00");
        gorro.getInsumos().add(productoInsumo(gorro, hilo, "2"));
        Venta venta = venta(5L, EstadoVenta.PRESUPUESTO, ventaItem(1L, 3, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(gorro));
        when(insumoRepository.findAllById(Set.of(1L))).thenReturn(List.of(hilo));

        assertThatThrownBy(() -> ventaService.actualizarEstado(5L, new ActualizarEstadoVentaRequest(EstadoVenta.CONFIRMADA), principal))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("Hilo");
        verify(insumoRepository, never()).save(any());
    }

    @Test
    void cancelarVentaDeProductoConRecetaRestauraStockDeCadaInsumo() {
        Insumo hilo = insumo(1L, "Hilo", "44");
        Producto gorro = producto(1L, "Gorro tejido", 0, "300.00");
        gorro.getInsumos().add(productoInsumo(gorro, hilo, "2"));
        Venta venta = venta(5L, EstadoVenta.CONFIRMADA, ventaItem(1L, 3, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(productoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(gorro));
        when(insumoRepository.findAllById(Set.of(1L))).thenReturn(List.of(hilo));

        ventaService.actualizarEstado(5L, new ActualizarEstadoVentaRequest(EstadoVenta.CANCELADA), principal);

        assertThat(hilo.getStock()).isEqualByComparingTo("50");
    }

    // --- textoCompartir() ---

    @Test
    void textoCompartirArmaElMensajeConItemsColorDescuentoYFechaDeEntrega() {
        Producto remera = producto(1L, "Remera", 0, "100.00");
        ProductoVariante rojo = variante(100L, remera, "Rojo", 5);
        remera.getVariantes().add(rojo);
        VentaItem item = ventaItem(1L, 2, 100L);
        item.setVarianteColor("Rojo");
        item.setPersonalizacion("Grabar Feliz Cumple");
        item.setGrabadoImagenUrl("/uploads/ventas/grabado.png");
        Venta venta = venta(5L, EstadoVenta.PRESUPUESTO, item);
        venta.setFechaEntrega(LocalDate.now());
        venta.setSubtotal(new BigDecimal("200.00"));
        venta.setDescuentoPorcentaje(new BigDecimal("10"));
        venta.setTotal(new BigDecimal("180.00"));
        Cliente cliente = cliente(1L, "Ana");
        cliente.setTelefono("1122334455");
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(clienteRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(cliente));

        TextoCompartirDto resultado = ventaService.textoCompartir(5L, principal);

        assertThat(resultado.telefonoCliente()).isEqualTo("1122334455");
        assertThat(resultado.texto())
                .contains("Ana")
                .contains("Rojo")
                .contains("Grabar Feliz Cumple")
                .contains("grabado.png")
                .contains("Descuento: 10%")
                .contains("Entrega estimada");
    }

    @Test
    void textoCompartirDeClienteInexistenteLanzaExcepcion() {
        Venta venta = venta(5L, EstadoVenta.PRESUPUESTO, ventaItem(1L, 1, null));
        when(ventaRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(venta));
        when(clienteRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ventaService.textoCompartir(5L, principal))
                .isInstanceOf(ClienteNoEncontradoException.class);
    }

    // --- subirFoto() ---

    @Test
    void subirFotoGuardaElArchivoYDevuelveLaUrl() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1000L);
        when(file.getContentType()).thenReturn("image/png");

        FotoUploadDto resultado = ventaService.subirFoto(file, principal);

        assertThat(resultado.fotoUrl()).startsWith("/uploads/ventas/").endsWith(".png");
    }

    @Test
    void subirFotoSinEmpresaLanzaExcepcion() {
        UserPrincipal superAdmin = new UserPrincipal(1L, null, true, null);

        assertThatThrownBy(() -> ventaService.subirFoto(mock(MultipartFile.class), superAdmin))
                .isInstanceOf(SinEmpresaException.class);
    }

    // --- Helpers ---

    private Insumo insumo(Long id, String nombre, String stock) {
        Insumo insumo = new Insumo();
        insumo.setId(id);
        insumo.setEmpresaId(EMPRESA_ID);
        insumo.setNombre(nombre);
        insumo.setCostoUnitario(BigDecimal.ONE);
        insumo.setStock(new BigDecimal(stock));
        insumo.setActivo(true);
        return insumo;
    }

    private ProductoInsumo productoInsumo(Producto producto, Insumo insumo, String cantidad) {
        ProductoInsumo productoInsumo = new ProductoInsumo();
        productoInsumo.setProducto(producto);
        productoInsumo.setInsumo(insumo);
        productoInsumo.setCantidad(new BigDecimal(cantidad));
        return productoInsumo;
    }

    private void mockearClienteYProductos(Producto... productos) {
        when(clienteRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(cliente(1L, "Ana")));
        for (Producto producto : productos) {
            when(productoRepository.findByIdAndEmpresaId(producto.getId(), EMPRESA_ID)).thenReturn(Optional.of(producto));
        }
    }

    private Producto producto(Long id, String nombre, int stock, String precioVenta) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setEmpresaId(EMPRESA_ID);
        producto.setNombre(nombre);
        producto.setPrecioVenta(new BigDecimal(precioVenta));
        producto.setStock(stock);
        producto.setActivo(true);
        return producto;
    }

    private ProductoComponente componente(Producto kit, Producto componenteProducto, int cantidad) {
        ProductoComponente componente = new ProductoComponente();
        componente.setProducto(kit);
        componente.setComponenteProducto(componenteProducto);
        componente.setCantidad(cantidad);
        return componente;
    }

    private ProductoVariante variante(Long id, Producto producto, String color, int stock) {
        ProductoVariante variante = new ProductoVariante();
        variante.setId(id);
        variante.setProducto(producto);
        variante.setColor(color);
        variante.setStock(stock);
        return variante;
    }

    private Cliente cliente(Long id, String nombre) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setEmpresaId(EMPRESA_ID);
        cliente.setNombre(nombre);
        cliente.setActivo(true);
        return cliente;
    }

    private VentaItemRequest itemRequest(Long productoId, int cantidad, BigDecimal precioUnitario, Long varianteId) {
        return new VentaItemRequest(productoId, cantidad, precioUnitario, null, null, varianteId);
    }

    private VentaRequest ventaRequest(BigDecimal descuentoPorcentaje, VentaItemRequest... items) {
        return new VentaRequest(1L, null, null, descuentoPorcentaje, List.of(items));
    }

    private VentaItem ventaItem(Long productoId, int cantidad, Long varianteId) {
        VentaItem item = new VentaItem();
        item.setProductoId(productoId);
        item.setProductoNombre("Item");
        item.setCantidad(cantidad);
        item.setPrecioUnitario(BigDecimal.TEN);
        item.setSubtotal(BigDecimal.TEN.multiply(BigDecimal.valueOf(cantidad)));
        item.setVarianteId(varianteId);
        return item;
    }

    private Venta venta(Long id, EstadoVenta estado, VentaItem... items) {
        Venta venta = new Venta();
        venta.setId(id);
        venta.setEmpresaId(EMPRESA_ID);
        venta.setClienteId(1L);
        venta.setEstado(estado);
        venta.setFechaPedido(LocalDateTime.now());
        venta.setFechaAlta(LocalDateTime.now());
        for (VentaItem item : items) {
            item.setVenta(venta);
            venta.getItems().add(item);
        }
        return venta;
    }
}
