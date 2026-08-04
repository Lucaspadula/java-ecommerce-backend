package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.DashboardResumenDto;
import com.sistventas.backend.dto.GananciaMesDto;
import com.sistventas.backend.dto.TopProductoDto;
import com.sistventas.backend.dto.UnidadesMesDto;
import com.sistventas.backend.entity.Cliente;
import com.sistventas.backend.entity.EstadoPedido;
import com.sistventas.backend.entity.EstadoVenta;
import com.sistventas.backend.entity.Insumo;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ProductoInsumo;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.Venta;
import com.sistventas.backend.entity.VentaItem;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.ClienteRepository;
import com.sistventas.backend.repository.InsumoRepository;
import com.sistventas.backend.repository.ProductoRepository;
import com.sistventas.backend.repository.ProveedorRepository;
import com.sistventas.backend.repository.VentaRepository;
import com.sistventas.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests de la lógica de negocio de DashboardServiceImpl: el resumen agrega,
// en memoria, una sola query de ventas no canceladas (VentaRepository.
// findByEmpresaIdAndEstadoNot) junto con counts puntuales de los demás
// repos. No es @SpringBootTest: todos los repositorios se mockean. El foco
// está en los cálculos con lógica real (ganancia/margen, agrupaciones por
// estado/mes, ranking de top productos, filtros de stock/entregas) y no en
// los simples pass-through a un count() del repository.
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock private VentaRepository ventaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private InsumoRepository insumoRepository;

    private DashboardServiceImpl service;

    private final UserPrincipal principal = new UserPrincipal(1L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @BeforeEach
    void setUp() {
        service = new DashboardServiceImpl(ventaRepository, clienteRepository, productoRepository,
                proveedorRepository, insumoRepository);

        // Defaults "empresa vacía": cada test de cálculo puntual sobreescribe
        // solo lo que necesita, sin tener que repetir todos los mocks.
        lenient().when(ventaRepository.findByEmpresaIdAndEstadoNot(EMPRESA_ID, EstadoVenta.CANCELADA))
                .thenReturn(List.of());
        lenient().when(productoRepository.findAllById(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of());
        lenient().when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of());
        lenient().when(insumoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of());
        lenient().when(clienteRepository.findAllById(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of());
        lenient().when(clienteRepository.countByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(0L);
        lenient().when(productoRepository.countByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(0L);
        lenient().when(proveedorRepository.countByEmpresaIdAndEstadoPedido(EMPRESA_ID, EstadoPedido.EN_PROGRESO)).thenReturn(0L);
    }

    // --- gananciaTotal / margenPromedioPorcentaje ---

    @Test
    void calculaGananciaYMargenComoIngresosMenosCostoDeReceta() {
        // Producto 1 tiene receta (1 insumo a $5 x 2 = costo unitario $10):
        // se vende x3 en la venta 1 => costo de esa línea $30.
        Producto productoConReceta = productoConReceta(1L, "Mate", new BigDecimal("5.00"), new BigDecimal("2"));
        // Producto 2 no tiene receta cargada (insumos vacío): el costo de
        // cualquier línea con este producto es cero para el dashboard, sin
        // importar que tenga costoUnitario propio cargado (ver nota más abajo).
        Producto productoSinReceta = productoConId(2L, "Vela");

        Venta v1 = venta(EstadoVenta.CONFIRMADA, new BigDecimal("100.00"));
        v1.getItems().add(item(productoConReceta.getId(), 3));
        Venta v2 = venta(EstadoVenta.ENTREGADA, new BigDecimal("200.00"));
        v2.getItems().add(item(productoSinReceta.getId(), 1));

        when(ventaRepository.findByEmpresaIdAndEstadoNot(EMPRESA_ID, EstadoVenta.CANCELADA))
                .thenReturn(List.of(v1, v2));
        when(productoRepository.findAllById(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(productoConReceta, productoSinReceta));

        DashboardResumenDto resumen = service.resumen(principal);

        assertThat(resumen.totalVentas()).isEqualByComparingTo("300.00");
        assertThat(resumen.gananciaTotal()).isEqualByComparingTo("270.00");
        assertThat(resumen.margenPromedioPorcentaje()).isEqualByComparingTo("90.00");
    }

    @Test
    void productoSinRecetaUsaSuCostoUnitarioPropioCargadoAMano() {
        // Fix: antes DashboardServiceImpl.costoItem solo sumaba
        // producto.getInsumos() y trataba el costo como CERO si la receta
        // estaba vacía, ignorando Producto.costoUnitario — a diferencia de
        // ProductoServiceImpl.costoEfectivo(), que sí lo usa como fallback.
        // Con productos "simples" (sin receta, costo cargado a mano) eso
        // sobreestimaba gananciaTotal/margenPromedioPorcentaje acá. Ahora
        // usa el mismo criterio en los dos lugares.
        Producto productoSinReceta = productoConId(2L, "Vela");
        productoSinReceta.setCostoUnitario(new BigDecimal("15.50"));

        Venta v1 = venta(EstadoVenta.CONFIRMADA, new BigDecimal("50.00"));
        v1.getItems().add(item(productoSinReceta.getId(), 2));

        when(ventaRepository.findByEmpresaIdAndEstadoNot(EMPRESA_ID, EstadoVenta.CANCELADA))
                .thenReturn(List.of(v1));
        when(productoRepository.findAllById(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(productoSinReceta));

        DashboardResumenDto resumen = service.resumen(principal);

        // Costo: 15.50 * 2 = 31.00. Ganancia: 50.00 - 31.00 = 19.00. Margen: 38%.
        assertThat(resumen.gananciaTotal()).isEqualByComparingTo("19.00");
        assertThat(resumen.margenPromedioPorcentaje()).isEqualByComparingTo("38.00");
    }

    @Test
    void productoSinRecetaYSinCostoUnitarioCargadoDaCostoCero() {
        Producto productoSinReceta = productoConId(3L, "Llavero");
        // costoUnitario queda null a propósito (no cargó nada).

        Venta v1 = venta(EstadoVenta.CONFIRMADA, new BigDecimal("20.00"));
        v1.getItems().add(item(productoSinReceta.getId(), 1));

        when(ventaRepository.findByEmpresaIdAndEstadoNot(EMPRESA_ID, EstadoVenta.CANCELADA))
                .thenReturn(List.of(v1));
        when(productoRepository.findAllById(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(productoSinReceta));

        DashboardResumenDto resumen = service.resumen(principal);

        assertThat(resumen.gananciaTotal()).isEqualByComparingTo("20.00");
        assertThat(resumen.margenPromedioPorcentaje()).isEqualByComparingTo("100.00");
    }

    @Test
    void sinVentasElMargenEsCeroYNoDivideNiExplota() {
        DashboardResumenDto resumen = service.resumen(principal);

        assertThat(resumen.totalVentas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resumen.gananciaTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resumen.margenPromedioPorcentaje()).isEqualByComparingTo("0.00");
    }

    // --- pedidosPorEstado (pipeline) ---

    @Test
    void agrupaVentasPorEstadoYExcluyeCanceladaDelPipeline() {
        Venta confirmada = venta(EstadoVenta.CONFIRMADA, new BigDecimal("10.00"));
        Venta enProceso1 = venta(EstadoVenta.EN_PROCESO, new BigDecimal("10.00"));
        Venta enProceso2 = venta(EstadoVenta.EN_PROCESO, new BigDecimal("10.00"));
        Venta entregada = venta(EstadoVenta.ENTREGADA, new BigDecimal("10.00"));

        when(ventaRepository.findByEmpresaIdAndEstadoNot(EMPRESA_ID, EstadoVenta.CANCELADA))
                .thenReturn(List.of(confirmada, enProceso1, enProceso2, entregada));

        DashboardResumenDto resumen = service.resumen(principal);

        // CANCELADA jamás debería figurar en el pipeline: ni la trae la query
        // (findByEmpresaIdAndEstadoNot), ni la deja pasar el filtro explícito
        // sobre EstadoVenta.values().
        assertThat(resumen.pedidosPorEstado())
                .extracting(dto -> dto.estado(), dto -> dto.cantidad())
                .containsExactly(
                        tuple(EstadoVenta.PRESUPUESTO, 0L),
                        tuple(EstadoVenta.CONFIRMADA, 1L),
                        tuple(EstadoVenta.EN_PROCESO, 2L),
                        tuple(EstadoVenta.LISTA, 0L),
                        tuple(EstadoVenta.ENTREGADA, 1L));
    }

    // --- productosSinStock / insumosStockBajo ---

    @Test
    void filtraProductosSinStockDejandoAfueraLosQueTienenStock() {
        Producto sinStock = productoConId(1L, "Mate sin stock");
        sinStock.setStock(0);
        Producto conStock = productoConId(2L, "Mate con stock");
        conStock.setStock(5);

        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID))
                .thenReturn(List.of(sinStock, conStock));

        DashboardResumenDto resumen = service.resumen(principal);

        assertThat(resumen.productosSinStock())
                .extracting(dto -> dto.id())
                .containsExactly(1L);
    }

    @Test
    void filtraInsumosPorStockMenorOIgualAlMinimoYExcluyeLosSinMinimoCargado() {
        Insumo bajo = insumo(1L, "Madera", new BigDecimal("2.000"), new BigDecimal("5.000"));
        Insumo justoEnElLimite = insumo(2L, "Tela", new BigDecimal("5.000"), new BigDecimal("5.000"));
        Insumo ok = insumo(3L, "Hilo", new BigDecimal("50.000"), new BigDecimal("5.000"));
        Insumo sinMinimoCargado = insumo(4L, "Barniz", new BigDecimal("1.000"), null);

        when(insumoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID))
                .thenReturn(List.of(bajo, justoEnElLimite, ok, sinMinimoCargado));

        DashboardResumenDto resumen = service.resumen(principal);

        assertThat(resumen.insumosStockBajo())
                .extracting(dto -> dto.id())
                .containsExactlyInAnyOrder(1L, 2L);
    }

    // --- topProductos ---

    @Test
    void ordenaTopProductosPorCantidadVendidaDescendente() {
        Producto masVendido = productoConId(1L, "Mate");
        Producto medio = productoConId(2L, "Termo");
        Producto menosVendido = productoConId(3L, "Bombilla");

        Venta v1 = venta(EstadoVenta.CONFIRMADA, new BigDecimal("10.00"));
        v1.getItems().add(item(masVendido.getId(), 10));
        v1.getItems().add(item(medio.getId(), 5));
        v1.getItems().add(item(menosVendido.getId(), 1));

        when(ventaRepository.findByEmpresaIdAndEstadoNot(EMPRESA_ID, EstadoVenta.CANCELADA))
                .thenReturn(List.of(v1));
        when(productoRepository.findAllById(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(masVendido, medio, menosVendido));

        DashboardResumenDto resumen = service.resumen(principal);

        assertThat(resumen.topProductos())
                .extracting(TopProductoDto::id, TopProductoDto::cantidadVendida)
                .containsExactly(
                        tuple(1L, 10),
                        tuple(2L, 5),
                        tuple(3L, 1));
    }

    // --- gananciaPorMes / unidadesPorMes ---

    @Test
    void gananciaYUnidadesPorMesTraenSiempreSeisMesesIncluyendoCeros() {
        Producto producto = productoConId(1L, "Mate");
        Venta esteMes = venta(EstadoVenta.CONFIRMADA, new BigDecimal("150.00"));
        esteMes.setFechaPedido(LocalDateTime.now());
        esteMes.getItems().add(item(producto.getId(), 4));

        when(ventaRepository.findByEmpresaIdAndEstadoNot(EMPRESA_ID, EstadoVenta.CANCELADA))
                .thenReturn(List.of(esteMes));
        when(productoRepository.findAllById(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(producto));

        DashboardResumenDto resumen = service.resumen(principal);

        String mesActual = YearMonth.now().toString();

        assertThat(resumen.gananciaPorMes()).hasSize(6);
        assertThat(resumen.unidadesPorMes()).hasSize(6);

        assertThat(resumen.gananciaPorMes())
                .extracting(GananciaMesDto::mes)
                .last().isEqualTo(mesActual);
        assertThat(resumen.gananciaPorMes())
                .filteredOn(dto -> dto.mes().equals(mesActual))
                .extracting(GananciaMesDto::ganancia)
                .containsExactly(new BigDecimal("150.00"));

        assertThat(resumen.unidadesPorMes())
                .filteredOn(dto -> dto.mes().equals(mesActual))
                .extracting(UnidadesMesDto::unidades)
                .containsExactly(4);

        // Los otros 5 meses no tuvieron ventas: deben venir en cero, no ausentes.
        assertThat(resumen.gananciaPorMes())
                .filteredOn(dto -> !dto.mes().equals(mesActual))
                .extracting(GananciaMesDto::ganancia)
                .allSatisfy(ganancia -> assertThat(ganancia).isEqualByComparingTo(BigDecimal.ZERO));
    }

    // --- proximasEntregas ---

    @Test
    void proximasEntregasExcluyeEntregadasYSinFechaYOrdenaPorFechaAscendente() {
        Cliente cliente = new Cliente();
        cliente.setId(9L);
        cliente.setNombre("Ana");
        when(clienteRepository.findAllById(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of(cliente));

        Venta lejana = venta(EstadoVenta.CONFIRMADA, new BigDecimal("10.00"));
        lejana.setClienteId(9L);
        lejana.setFechaEntrega(LocalDate.now().plusDays(10));

        Venta cercana = venta(EstadoVenta.EN_PROCESO, new BigDecimal("10.00"));
        cercana.setClienteId(9L);
        cercana.setFechaEntrega(LocalDate.now().plusDays(2));

        Venta yaEntregada = venta(EstadoVenta.ENTREGADA, new BigDecimal("10.00"));
        yaEntregada.setClienteId(9L);
        yaEntregada.setFechaEntrega(LocalDate.now().plusDays(1));

        Venta sinFecha = venta(EstadoVenta.LISTA, new BigDecimal("10.00"));
        sinFecha.setClienteId(9L);
        sinFecha.setFechaEntrega(null);

        when(ventaRepository.findByEmpresaIdAndEstadoNot(EMPRESA_ID, EstadoVenta.CANCELADA))
                .thenReturn(List.of(lejana, cercana, yaEntregada, sinFecha));

        DashboardResumenDto resumen = service.resumen(principal);

        assertThat(resumen.proximasEntregas())
                .extracting(dto -> dto.fechaEntrega())
                .containsExactly(cercana.getFechaEntrega(), lejana.getFechaEntrega());
        assertThat(resumen.proximasEntregas())
                .extracting(dto -> dto.clienteNombre())
                .containsOnly("Ana");
    }

    // --- empresa nueva sin datos ---

    @Test
    void empresaNuevaSinDatosDevuelveCerosYListasVaciasSinExplotar() {
        DashboardResumenDto resumen = service.resumen(principal);

        assertThat(resumen.totalVentas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resumen.gananciaTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resumen.margenPromedioPorcentaje()).isEqualByComparingTo("0.00");
        assertThat(resumen.clientesActivos()).isZero();
        assertThat(resumen.productosActivos()).isZero();
        assertThat(resumen.proveedoresConPedidoEnCurso()).isZero();
        assertThat(resumen.productosSinStock()).isEmpty();
        assertThat(resumen.insumosStockBajo()).isEmpty();
        assertThat(resumen.topProductos()).isEmpty();
        assertThat(resumen.proximasEntregas()).isEmpty();
        assertThat(resumen.gananciaPorMes()).hasSize(6);
        assertThat(resumen.unidadesPorMes()).hasSize(6);
        assertThat(resumen.pedidosPorEstado()).hasSize(5);
        assertThat(resumen.pedidosPorEstado()).extracting(dto -> dto.cantidad()).containsOnly(0L);
    }

    // --- multi-tenant ---

    @Test
    void todasLasConsultasFiltranPorEmpresaIdDelPrincipal() {
        service.resumen(principal);

        verify(ventaRepository).findByEmpresaIdAndEstadoNot(eq(EMPRESA_ID), eq(EstadoVenta.CANCELADA));
        verify(productoRepository).findByEmpresaIdAndActivoTrue(eq(EMPRESA_ID));
        verify(insumoRepository).findByEmpresaIdAndActivoTrue(eq(EMPRESA_ID));
        verify(clienteRepository).countByEmpresaIdAndActivoTrue(eq(EMPRESA_ID));
        verify(productoRepository).countByEmpresaIdAndActivoTrue(eq(EMPRESA_ID));
        verify(proveedorRepository).countByEmpresaIdAndEstadoPedido(eq(EMPRESA_ID), eq(EstadoPedido.EN_PROGRESO));
    }

    @Test
    void sinEmpresaIdEnElPrincipalLanzaSinEmpresaException() {
        UserPrincipal superAdmin = new UserPrincipal(1L, null, true, null);

        assertThatThrownBy(() -> service.resumen(superAdmin))
                .isInstanceOf(SinEmpresaException.class);
    }

    // --- Helpers ---

    private Venta venta(EstadoVenta estado, BigDecimal total) {
        Venta venta = new Venta();
        venta.setEmpresaId(EMPRESA_ID);
        venta.setClienteId(1L);
        venta.setEstado(estado);
        venta.setTotal(total);
        venta.setFechaPedido(LocalDateTime.now());
        return venta;
    }

    private VentaItem item(Long productoId, int cantidad) {
        VentaItem item = new VentaItem();
        item.setProductoId(productoId);
        item.setProductoNombre("Item");
        item.setCantidad(cantidad);
        item.setPrecioUnitario(BigDecimal.ZERO);
        item.setSubtotal(BigDecimal.ZERO);
        return item;
    }

    private Producto productoConId(Long id, String nombre) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setStock(0);
        return producto;
    }

    private Producto productoConReceta(Long id, String nombre, BigDecimal costoUnitarioInsumo, BigDecimal cantidadReceta) {
        Producto producto = productoConId(id, nombre);
        Insumo insumo = new Insumo();
        insumo.setId(100L + id);
        insumo.setNombre("Insumo de " + nombre);
        insumo.setCostoUnitario(costoUnitarioInsumo);
        insumo.setStock(new BigDecimal("100"));

        ProductoInsumo productoInsumo = new ProductoInsumo();
        productoInsumo.setProducto(producto);
        productoInsumo.setInsumo(insumo);
        productoInsumo.setCantidad(cantidadReceta);
        producto.getInsumos().add(productoInsumo);
        return producto;
    }

    private Insumo insumo(Long id, String nombre, BigDecimal stock, BigDecimal stockMinimo) {
        Insumo insumo = new Insumo();
        insumo.setId(id);
        insumo.setNombre(nombre);
        insumo.setCostoUnitario(BigDecimal.ONE);
        insumo.setStock(stock);
        insumo.setStockMinimo(stockMinimo);
        return insumo;
    }
}
