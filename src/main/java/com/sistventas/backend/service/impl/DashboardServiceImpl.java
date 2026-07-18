package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.DashboardResumenDto;
import com.sistventas.backend.dto.EstadoConteoDto;
import com.sistventas.backend.dto.GananciaMesDto;
import com.sistventas.backend.dto.InsumoStockBajoDto;
import com.sistventas.backend.dto.ProductoSinStockDto;
import com.sistventas.backend.dto.TopProductoDto;
import com.sistventas.backend.dto.UnidadesMesDto;
import com.sistventas.backend.dto.VentaResumenDto;
import com.sistventas.backend.entity.Cliente;
import com.sistventas.backend.entity.EstadoPedido;
import com.sistventas.backend.entity.EstadoVenta;
import com.sistventas.backend.entity.Insumo;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.Venta;
import com.sistventas.backend.entity.VentaItem;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.ClienteRepository;
import com.sistventas.backend.repository.InsumoRepository;
import com.sistventas.backend.repository.ProductoRepository;
import com.sistventas.backend.repository.ProveedorRepository;
import com.sistventas.backend.repository.VentaRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final InsumoRepository insumoRepository;

    public DashboardServiceImpl(VentaRepository ventaRepository,
                                 ClienteRepository clienteRepository,
                                 ProductoRepository productoRepository,
                                 ProveedorRepository proveedorRepository,
                                 InsumoRepository insumoRepository) {
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.insumoRepository = insumoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResumenDto resumen(UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);

        // Volumen chico de datos en este proyecto: se resuelve todo en memoria
        // sobre una sola query de ventas no canceladas, en vez de agregaciones
        // JPQL separadas. Mismo criterio de batch-fetch que VentaServiceImpl.
        List<Venta> ventas = ventaRepository.findByEmpresaIdAndEstadoNot(empresaId, EstadoVenta.CANCELADA);

        BigDecimal totalVentas = ventas.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<VentaItem> items = ventas.stream()
                .flatMap(venta -> venta.getItems().stream())
                .toList();

        List<Long> productoIds = items.stream()
                .map(VentaItem::getProductoId)
                .distinct()
                .toList();
        Map<Long, Producto> productosPorId = productoRepository.findAllById(productoIds).stream()
                .collect(Collectors.toMap(Producto::getId, producto -> producto));

        BigDecimal costoTotal = items.stream()
                .map(item -> costoItem(item, productosPorId))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gananciaTotal = totalVentas.subtract(costoTotal);

        BigDecimal margenPromedioPorcentaje = totalVentas.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : gananciaTotal.divide(totalVentas, 10, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);

        Map<EstadoVenta, Long> conteoPorEstado = ventas.stream()
                .collect(Collectors.groupingBy(Venta::getEstado, Collectors.counting()));
        List<EstadoConteoDto> pedidosPorEstado = Arrays.stream(EstadoVenta.values())
                .filter(estado -> estado != EstadoVenta.CANCELADA)
                .map(estado -> new EstadoConteoDto(estado, conteoPorEstado.getOrDefault(estado, 0L)))
                .toList();

        long clientesActivos = clienteRepository.countByEmpresaIdAndActivoTrue(empresaId);
        long productosActivos = productoRepository.countByEmpresaIdAndActivoTrue(empresaId);
        long proveedoresConPedidoEnCurso = proveedorRepository.countByEmpresaIdAndEstadoPedido(empresaId, EstadoPedido.EN_PROGRESO);

        List<VentaResumenDto> proximasEntregas = proximasEntregas(ventas);
        List<ProductoSinStockDto> productosSinStock = productosSinStock(empresaId);
        List<InsumoStockBajoDto> insumosStockBajo = insumosStockBajo(empresaId);
        List<TopProductoDto> topProductos = topProductos(items, productosPorId);
        List<GananciaMesDto> gananciaPorMes = gananciaPorMes(ventas, productosPorId);
        List<UnidadesMesDto> unidadesPorMes = unidadesPorMes(ventas);

        return new DashboardResumenDto(
                totalVentas,
                gananciaTotal,
                margenPromedioPorcentaje,
                pedidosPorEstado,
                clientesActivos,
                productosActivos,
                proveedoresConPedidoEnCurso,
                proximasEntregas,
                productosSinStock,
                insumosStockBajo,
                topProductos,
                gananciaPorMes,
                unidadesPorMes
        );
    }

    private List<ProductoSinStockDto> productosSinStock(Long empresaId) {
        return productoRepository.findByEmpresaIdAndActivoTrue(empresaId).stream()
                .filter(producto -> producto.getStock() != null && producto.getStock() == 0)
                .map(producto -> new ProductoSinStockDto(producto.getId(), producto.getNombre()))
                .toList();
    }

    private List<InsumoStockBajoDto> insumosStockBajo(Long empresaId) {
        return insumoRepository.findByEmpresaIdAndActivoTrue(empresaId).stream()
                .filter(insumo -> insumo.getStockMinimo() != null
                        && insumo.getStock().compareTo(insumo.getStockMinimo()) <= 0)
                .map(insumo -> new InsumoStockBajoDto(
                        insumo.getId(), insumo.getNombre(), insumo.getStock(), insumo.getStockMinimo()))
                .toList();
    }

    // Ranking por unidades vendidas (no por facturación): a un revendedor le
    // sirve más saber qué mueve volumen, no solo qué genera más plata.
    private List<TopProductoDto> topProductos(List<VentaItem> items, Map<Long, Producto> productosPorId) {
        Map<Long, List<VentaItem>> itemsPorProducto = items.stream()
                .collect(Collectors.groupingBy(VentaItem::getProductoId));

        return itemsPorProducto.entrySet().stream()
                .map(entry -> {
                    Producto producto = productosPorId.get(entry.getKey());
                    if (producto == null) {
                        return null;
                    }
                    int cantidadVendida = entry.getValue().stream().mapToInt(VentaItem::getCantidad).sum();
                    BigDecimal gananciaProducto = entry.getValue().stream()
                            .map(item -> item.getSubtotal().subtract(costoItem(item, productosPorId)))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new TopProductoDto(producto.getId(), producto.getNombre(), cantidadVendida, gananciaProducto);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(TopProductoDto::cantidadVendida).reversed())
                .limit(5)
                .toList();
    }

    // Últimos 6 meses (incluye meses en cero) para poder graficar una
    // tendencia real, no solo los meses en los que hubo ventas.
    private List<GananciaMesDto> gananciaPorMes(List<Venta> ventas, Map<Long, Producto> productosPorId) {
        Map<YearMonth, BigDecimal> gananciaPorMes = new HashMap<>();
        for (Venta venta : ventas) {
            YearMonth mes = YearMonth.from(venta.getFechaPedido());
            BigDecimal costoVenta = venta.getItems().stream()
                    .map(item -> costoItem(item, productosPorId))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal gananciaVenta = venta.getTotal().subtract(costoVenta);
            gananciaPorMes.merge(mes, gananciaVenta, BigDecimal::add);
        }

        YearMonth mesActual = YearMonth.now();
        return IntStream.rangeClosed(0, 5)
                .mapToObj(i -> mesActual.minusMonths(5 - i))
                .map(mes -> new GananciaMesDto(mes.toString(), gananciaPorMes.getOrDefault(mes, BigDecimal.ZERO)))
                .toList();
    }

    // Mismo criterio que gananciaPorMes (últimos 6 meses, incluye ceros) pero
    // contando unidades vendidas en vez de plata — sirve para ver volumen de
    // movimiento independiente del ticket promedio de cada venta.
    private List<UnidadesMesDto> unidadesPorMes(List<Venta> ventas) {
        Map<YearMonth, Integer> unidadesPorMes = new HashMap<>();
        for (Venta venta : ventas) {
            YearMonth mes = YearMonth.from(venta.getFechaPedido());
            int unidadesVenta = venta.getItems().stream().mapToInt(VentaItem::getCantidad).sum();
            unidadesPorMes.merge(mes, unidadesVenta, Integer::sum);
        }

        YearMonth mesActual = YearMonth.now();
        return IntStream.rangeClosed(0, 5)
                .mapToObj(i -> mesActual.minusMonths(5 - i))
                .map(mes -> new UnidadesMesDto(mes.toString(), unidadesPorMes.getOrDefault(mes, 0)))
                .toList();
    }

    // Costo unitario del producto = suma, por cada línea de receta, del costo
    // unitario del insumo maestro multiplicado por la cantidad usada de ese
    // insumo (Etapa 1: Insumo pasó a ser entidad maestra con costoUnitario
    // propio; ProductoInsumo ya no guarda un costo fijo por línea). No hay
    // campo ni método precalculado en Producto/ProductoInsumo, así que se
    // itera acá. Si el producto ya no se puede resolver (dato histórico sin
    // producto vivo), el costo se trata como cero: este es un endpoint de
    // reporting y no debe romperse por datos viejos.
    private BigDecimal costoItem(VentaItem item, Map<Long, Producto> productosPorId) {
        Producto producto = productosPorId.get(item.getProductoId());
        if (producto == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal costoUnitario = producto.getInsumos().stream()
                .map(productoInsumo -> productoInsumo.getInsumo().getCostoUnitario()
                        .multiply(productoInsumo.getCantidad()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return costoUnitario.multiply(BigDecimal.valueOf(item.getCantidad()));
    }

    private List<VentaResumenDto> proximasEntregas(List<Venta> ventas) {
        List<Venta> proximas = ventas.stream()
                .filter(venta -> venta.getEstado() != EstadoVenta.ENTREGADA && venta.getFechaEntrega() != null)
                .sorted(Comparator.comparing(Venta::getFechaEntrega))
                .limit(5)
                .toList();

        List<Long> clienteIds = proximas.stream().map(Venta::getClienteId).distinct().toList();
        Map<Long, String> nombresPorCliente = clienteRepository.findAllById(clienteIds).stream()
                .collect(Collectors.toMap(Cliente::getId, Cliente::getNombre));

        return proximas.stream()
                .map(venta -> new VentaResumenDto(
                        venta.getId(),
                        nombresPorCliente.get(venta.getClienteId()),
                        venta.getFechaEntrega(),
                        venta.getTotal(),
                        venta.getEstado()
                ))
                .toList();
    }

    // Único punto donde se resuelve empresaId del usuario logueado. Nunca se
    // acepta un empresaId del cliente (body/query) — siempre sale del
    // UserPrincipal armado en JwtAuthenticationFilter a partir del JWT.
    private Long empresaIdOrThrow(UserPrincipal principal) {
        if (principal == null || principal.empresaId() == null) {
            throw new SinEmpresaException();
        }
        return principal.empresaId();
    }
}
