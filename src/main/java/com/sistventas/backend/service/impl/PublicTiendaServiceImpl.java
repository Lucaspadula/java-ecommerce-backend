package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.BannerImagenPublicaDto;
import com.sistventas.backend.dto.CategoriaTiendaDto;
import com.sistventas.backend.dto.PreviewDescuentoComboDto;
import com.sistventas.backend.dto.PreviewDescuentoComboRequest;
import com.sistventas.backend.dto.PublicEmpresaDto;
import com.sistventas.backend.dto.PublicPedidoEstadoDto;
import com.sistventas.backend.dto.PublicPedidoHistorialDto;
import com.sistventas.backend.dto.PublicPedidoItemRequest;
import com.sistventas.backend.dto.PublicPedidoRequest;
import com.sistventas.backend.dto.PublicPedidoResultadoDto;
import com.sistventas.backend.dto.PublicProductoDto;
import com.sistventas.backend.dto.PublicTestimonioDto;
import com.sistventas.backend.dto.PublicTipDto;
import com.sistventas.backend.dto.ResenaDestacadaDto;
import com.sistventas.backend.dto.ResenaDto;
import com.sistventas.backend.entity.Categoria;
import com.sistventas.backend.entity.Cliente;
import com.sistventas.backend.entity.Empresa;
import com.sistventas.backend.entity.EstadoVenta;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ReglaDescuentoCombo;
import com.sistventas.backend.entity.Resena;
import com.sistventas.backend.entity.Subcategoria;
import com.sistventas.backend.entity.TiendaBannerImagen;
import com.sistventas.backend.entity.TiendaCategoria;
import com.sistventas.backend.entity.TiendaTestimonio;
import com.sistventas.backend.entity.Venta;
import com.sistventas.backend.entity.VentaItem;
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
import com.sistventas.backend.service.PublicTiendaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Vidriera pública (Fase 1, sin pago online): cualquiera puede ver el
 * catálogo de una empresa habilitada y armar un pedido, que queda como una
 * Venta en PRESUPUESTO para que el dueño la revise y confirme manualmente.
 * Ningún método recibe UserPrincipal: estas rutas no tienen JWT (ver
 * SecurityConfig, /api/public/** es permitAll), así que la empresa siempre
 * se resuelve a partir del slug de la URL, nunca de un id que mande el
 * cliente.
 */
@Service
public class PublicTiendaServiceImpl implements PublicTiendaService {

    private final EmpresaRepository empresaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final VentaRepository ventaRepository;
    private final VentaEstadoHistorialRepository ventaEstadoHistorialRepository;
    private final TiendaCategoriaRepository tiendaCategoriaRepository;
    private final CategoriaRepository categoriaRepository;
    private final TiendaBannerImagenRepository tiendaBannerImagenRepository;
    private final ResenaRepository resenaRepository;
    private final TiendaTestimonioRepository tiendaTestimonioRepository;
    private final TiendaTipRepository tiendaTipRepository;
    private final StockDisponibleCalculator stockDisponibleCalculator;
    private final ReglaDescuentoComboRepository reglaDescuentoComboRepository;
    private final CalculadorDescuentoComboService calculadorDescuentoComboService;

    public PublicTiendaServiceImpl(EmpresaRepository empresaRepository,
                                    ProductoRepository productoRepository,
                                    ClienteRepository clienteRepository,
                                    VentaRepository ventaRepository,
                                    VentaEstadoHistorialRepository ventaEstadoHistorialRepository,
                                    TiendaCategoriaRepository tiendaCategoriaRepository,
                                    CategoriaRepository categoriaRepository,
                                    TiendaBannerImagenRepository tiendaBannerImagenRepository,
                                    ResenaRepository resenaRepository,
                                    TiendaTestimonioRepository tiendaTestimonioRepository,
                                    TiendaTipRepository tiendaTipRepository,
                                    StockDisponibleCalculator stockDisponibleCalculator,
                                    ReglaDescuentoComboRepository reglaDescuentoComboRepository,
                                    CalculadorDescuentoComboService calculadorDescuentoComboService) {
        this.empresaRepository = empresaRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.ventaRepository = ventaRepository;
        this.ventaEstadoHistorialRepository = ventaEstadoHistorialRepository;
        this.tiendaCategoriaRepository = tiendaCategoriaRepository;
        this.categoriaRepository = categoriaRepository;
        this.tiendaBannerImagenRepository = tiendaBannerImagenRepository;
        this.resenaRepository = resenaRepository;
        this.tiendaTestimonioRepository = tiendaTestimonioRepository;
        this.tiendaTipRepository = tiendaTipRepository;
        this.stockDisponibleCalculator = stockDisponibleCalculator;
        this.reglaDescuentoComboRepository = reglaDescuentoComboRepository;
        this.calculadorDescuentoComboService = calculadorDescuentoComboService;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicEmpresaDto obtenerEmpresa(String slug) {
        Empresa empresa = resolverEmpresa(slug);
        List<BannerImagenPublicaDto> bannerImagenes =
                tiendaBannerImagenRepository.findByEmpresaIdAndTipoOrderByOrden(empresa.getId(), "HERO").stream()
                        .map(this::toBannerImagenPublicaDto)
                        .toList();
        List<BannerImagenPublicaDto> bannerVerticales =
                tiendaBannerImagenRepository.findByEmpresaIdAndTipoOrderByOrden(empresa.getId(), "VERTICAL").stream()
                        .map(this::toBannerImagenPublicaDto)
                        .toList();
        return new PublicEmpresaDto(
                empresa.getNombre(),
                empresa.getLogoUrl(),
                empresa.getTiendaBannerTitulo(),
                empresa.getTiendaBannerDescripcion(),
                empresa.getTiendaBannerTagline(),
                bannerImagenes,
                empresa.getTiendaContactoWhatsapp(),
                empresa.getTiendaContactoInstagram(),
                empresa.getTiendaContactoEmail(),
                empresa.getTiendaFuente(),
                bannerVerticales,
                empresa.getTiendaBannerVerticalPosicion(),
                empresa.getTiendaRazonSocial(),
                empresa.getTiendaCuit(),
                empresa.getTiendaDireccion(),
                empresa.getTiendaSobreNosotros()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicProductoDto> listarProductos(String slug) {
        Empresa empresa = resolverEmpresa(slug);

        // Resuelve la reseña MÁS RECIENTE de cada producto en un solo query
        // (no un N+1 por producto): la lista ya viene ordenada por fecha
        // desc, así que toMap con merge (a, b) -> a se queda con la primera
        // ocurrencia de cada productoId, que es la más reciente.
        Map<Long, Resena> resenaMasRecientePorProducto = resenaRepository.findByEmpresaIdOrderByFechaDesc(empresa.getId())
                .stream()
                .collect(Collectors.toMap(Resena::getProductoId, Function.identity(), (a, b) -> a));

        return productoRepository.findByEmpresaIdAndActivoTrue(empresa.getId()).stream()
                // No se muestra lo que no hay stock disponible. Usa el mismo
                // cálculo que ProductoServiceImpl (StockDisponibleCalculator):
                // para productos con receta esto no es Producto.stock, es lo
                // que alcanza a armar según el stock de insumos.
                .filter(producto -> stockDisponibleCalculator.calcular(producto) > 0)
                .map(producto -> toProductoDto(producto, resenaMasRecientePorProducto.get(producto.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaTiendaDto> listarCategorias(String slug) {
        Empresa empresa = resolverEmpresa(slug);
        Long empresaId = empresa.getId();

        // Solo categorías reales (de productos activos) que además tienen
        // color y/o imagen configurados: las sin configurar no se incluyen,
        // el frontend público ya tiene un fallback por hash para esas.
        Set<Long> categoriaIdsReales = productoRepository.findByEmpresaIdAndActivoTrue(empresaId).stream()
                .map(producto -> producto.getCategoria().getId())
                .collect(Collectors.toSet());

        // Mapa id -> Categoria de la empresa entera (no solo las reales):
        // más simple que resolver una por una, y el volumen por empresa es
        // chico. Ver toCategoriaDto para el nombre resuelto.
        Map<Long, Categoria> categoriasPorId = categoriaRepository.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(Categoria::getId, Function.identity()));

        return tiendaCategoriaRepository.findByEmpresaId(empresaId).stream()
                .filter(c -> categoriaIdsReales.contains(c.getCategoriaId()))
                .filter(c -> c.getColor() != null || c.getImagenUrl() != null)
                .map(c -> toCategoriaDto(c, categoriasPorId.get(c.getCategoriaId())))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResenaDto> listarResenas(String slug, Long productoId) {
        Empresa empresa = resolverEmpresa(slug);
        Producto producto = buscarProductoActivo(productoId, empresa.getId());
        return resenaRepository.findByProductoIdOrderByFechaDesc(producto.getId()).stream()
                .map(r -> new ResenaDto(r.getId(), r.getClienteNombre(), r.getComentario(), r.getFecha(), r.getImagenUrl()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicTestimonioDto> listarTestimonios(String slug) {
        Empresa empresa = resolverEmpresa(slug);
        return tiendaTestimonioRepository.findByEmpresaIdOrderByOrdenAscIdAsc(empresa.getId()).stream()
                .map(t -> new PublicTestimonioDto(t.getClienteNombre(), t.getComentario(), t.getFotoUrl(), t.getCanal()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicTipDto> listarTips(String slug) {
        Empresa empresa = resolverEmpresa(slug);
        return tiendaTipRepository.findByEmpresaIdOrderByOrdenAscIdAsc(empresa.getId()).stream()
                .map(t -> new PublicTipDto(t.getTitulo(), t.getContenido(), t.getFotoUrl()))
                .toList();
    }

    @Override
    @Transactional
    public PublicPedidoResultadoDto crearPedido(String slug, PublicPedidoRequest request) {
        Empresa empresa = resolverEmpresa(slug);
        Long empresaId = empresa.getId();

        Cliente cliente = resolverCliente(request, empresaId);

        Venta venta = new Venta();
        venta.setEmpresaId(empresaId);
        venta.setClienteId(cliente.getId());
        venta.setEstado(EstadoVenta.PRESUPUESTO);
        venta.setFechaPedido(LocalDateTime.now());
        venta.setFechaAlta(LocalDateTime.now());
        venta.setDescuentoPorcentaje(BigDecimal.ZERO);
        // Puramente informativos (sin cálculo de costo de envío): se guardan
        // tal cual para que el dueño los tenga a mano al coordinar por
        // WhatsApp. Vacío/solo-espacios se guarda como null, mismo criterio
        // que PerfilServiceImpl.vacioComoNull.
        venta.setDireccionEnvio(vacioComoNull(request.direccionEnvio()));
        venta.setLocalidad(vacioComoNull(request.localidad()));
        venta.setCodigoPostal(vacioComoNull(request.codigoPostal()));

        BigDecimal total = BigDecimal.ZERO;
        // Se arma en paralelo al loop de items: es el input del motor de
        // descuento combo (CalculadorDescuentoComboService), con la
        // categoría/subcategoría REAL de cada producto persistido — nunca la
        // que pudiera mandar el cliente.
        List<LineaCarritoCombo> lineasCombo = new ArrayList<>();
        for (PublicPedidoItemRequest itemRequest : request.items()) {
            Producto producto = buscarProductoActivo(itemRequest.productoId(), empresaId);

            // Mismo criterio que el catálogo (listarProductos): para
            // productos con receta, el disponible sale del stock de
            // insumos, no de Producto.stock.
            int disponible = stockDisponibleCalculator.calcular(producto);
            if (disponible < itemRequest.cantidad()) {
                throw new StockInsuficienteException(producto.getNombre(), disponible, itemRequest.cantidad());
            }

            // Nunca se acepta un precio que venga del pedido público: siempre
            // sale del producto persistido.
            BigDecimal precioUnitario = producto.getPrecioVenta();
            BigDecimal subtotalItem = precioUnitario.multiply(BigDecimal.valueOf(itemRequest.cantidad()));

            VentaItem item = new VentaItem();
            item.setVenta(venta);
            item.setProductoId(producto.getId());
            item.setProductoNombre(producto.getNombre());
            item.setCantidad(itemRequest.cantidad());
            item.setPrecioUnitario(precioUnitario);
            item.setSubtotal(subtotalItem);
            venta.getItems().add(item);

            total = total.add(subtotalItem);
            lineasCombo.add(toLineaCarritoCombo(producto, itemRequest.cantidad(), precioUnitario));
        }

        // PRESUPUESTO no reserva stock: eso pasa recién cuando el dueño lo
        // confirma desde el sistema (lógica ya existente en
        // VentaServiceImpl, no se toca acá).
        venta.setSubtotal(total);

        BigDecimal totalFinal = total;

        // 1) Descuento combo: recalculado 100% server-side (reglas activas de
        // la empresa + categoría real de cada producto), nunca a partir de un
        // monto que hubiera mandado el cliente. Se resta primero, antes del
        // cupón (ver punto 2).
        List<ReglaDescuentoCombo> reglasActivas = reglaDescuentoComboRepository.findByEmpresaIdAndActivoTrue(empresaId);
        ResultadoDescuentoCombo resultadoCombo = calculadorDescuentoComboService.calcular(lineasCombo, reglasActivas);
        if (resultadoCombo.montoDescuento().compareTo(BigDecimal.ZERO) > 0) {
            venta.setDescuentoComboMonto(resultadoCombo.montoDescuento());
            venta.setDescuentoComboDetalle(resultadoCombo.detalle());
            totalFinal = totalFinal.subtract(resultadoCombo.montoDescuento()).setScale(2, RoundingMode.HALF_UP);
        }

        // 2) Cupón: se ACUMULA con el combo (decisión de negocio confirmada),
        // no son excluyentes — se aplica sobre el total que ya quedó con el
        // descuento combo restado, no sobre el subtotal original.
        String cuponCodigo = request.cuponCodigo();
        if (cuponCodigo != null && !cuponCodigo.isBlank()) {
            boolean coincide = empresa.getTiendaCuponCodigo() != null
                    && empresa.getTiendaCuponCodigo().trim().equalsIgnoreCase(cuponCodigo.trim());
            if (!coincide || empresa.getTiendaCuponPorcentaje() == null) {
                throw new CuponInvalidoException();
            }
            BigDecimal porcentaje = empresa.getTiendaCuponPorcentaje();
            venta.setDescuentoPorcentaje(porcentaje);
            totalFinal = totalFinal
                    .multiply(BigDecimal.ONE.subtract(porcentaje.divide(BigDecimal.valueOf(100))))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        venta.setTotal(totalFinal);

        Venta guardada = ventaRepository.save(venta);
        return new PublicPedidoResultadoDto(guardada.getId(), guardada.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewDescuentoComboDto previewDescuentoCombo(String slug, PreviewDescuentoComboRequest request) {
        Empresa empresa = resolverEmpresa(slug);
        Long empresaId = empresa.getId();

        // Mismo criterio defensivo que crearPedido: precio y categoría
        // siempre se resuelven contra el producto persistido, el cliente
        // solo manda productoId + cantidad.
        List<LineaCarritoCombo> lineas = request.items().stream()
                .map(itemRequest -> {
                    Producto producto = buscarProductoActivo(itemRequest.productoId(), empresaId);
                    return toLineaCarritoCombo(producto, itemRequest.cantidad(), producto.getPrecioVenta());
                })
                .toList();

        List<ReglaDescuentoCombo> reglasActivas = reglaDescuentoComboRepository.findByEmpresaIdAndActivoTrue(empresaId);
        ResultadoDescuentoCombo resultado = calculadorDescuentoComboService.calcular(lineas, reglasActivas);

        if (resultado.montoDescuento().compareTo(BigDecimal.ZERO) <= 0) {
            return PreviewDescuentoComboDto.sinDescuento();
        }
        return new PreviewDescuentoComboDto(
                true,
                resultado.detalle(),
                resultado.pares(),
                resultado.reglaAplicada().getPorcentaje(),
                resultado.montoDescuento()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PublicPedidoEstadoDto consultarPedido(String slug, Long ventaId, String telefono) {
        Empresa empresa = resolverEmpresa(slug);

        Venta venta = ventaRepository.findByIdAndEmpresaId(ventaId, empresa.getId())
                .orElseThrow(VentaNoEncontradaException::new);

        // Mismo error que "venta no encontrada" si el teléfono no coincide o
        // el cliente no aparece: así el endpoint no sirve para adivinar si
        // un número de pedido existe probando teléfonos al azar.
        Cliente cliente = clienteRepository.findByIdAndEmpresaId(venta.getClienteId(), empresa.getId())
                .orElseThrow(VentaNoEncontradaException::new);
        String telefonoRecibido = telefono != null ? telefono.trim() : "";
        String telefonoCliente = cliente.getTelefono() != null ? cliente.getTelefono().trim() : "";
        if (!telefonoCliente.equals(telefonoRecibido)) {
            throw new VentaNoEncontradaException();
        }

        List<PublicPedidoHistorialDto> historial = ventaEstadoHistorialRepository
                .findByVentaIdOrderByFechaDescIdDesc(ventaId).stream()
                .map(h -> new PublicPedidoHistorialDto(h.getEstadoAnterior(), h.getEstadoNuevo(), h.getFecha()))
                .toList();

        return new PublicPedidoEstadoDto(
                venta.getId(),
                venta.getEstado(),
                venta.getFechaPedido(),
                venta.getFechaEntrega(),
                venta.getTotal(),
                historial
        );
    }

    // Un input vacío/solo-espacios se guarda como null: mismo criterio que
    // PerfilServiceImpl.vacioComoNull (así se distingue "no cargado" de
    // "cargado con texto vacío", sin que le importe a quien lo consuma).
    private String vacioComoNull(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }

    // Busca la empresa por slug + tienda habilitada. Compartido por los 3
    // métodos públicos: mismo chequeo, mismo 404 si no existe/deshabilitada.
    private Empresa resolverEmpresa(String slug) {
        return empresaRepository.findBySlugAndTiendaHabilitadaTrue(slug)
                .orElseThrow(TiendaNoEncontradaException::new);
    }

    // Evita duplicar un Cliente que ya escribió antes: busca por teléfono
    // dentro de la misma empresa: si no existe, lo crea.
    private Cliente resolverCliente(PublicPedidoRequest request, Long empresaId) {
        return clienteRepository.findByEmpresaIdAndTelefono(empresaId, request.clienteTelefono())
                .orElseGet(() -> {
                    Cliente nuevo = new Cliente();
                    nuevo.setEmpresaId(empresaId);
                    nuevo.setNombre(request.clienteNombre());
                    nuevo.setTelefono(request.clienteTelefono());
                    nuevo.setActivo(true);
                    nuevo.setFechaAlta(LocalDateTime.now());
                    return clienteRepository.save(nuevo);
                });
    }

    private Producto buscarProductoActivo(Long productoId, Long empresaId) {
        Producto producto = productoRepository.findByIdAndEmpresaId(productoId, empresaId)
                .orElseThrow(ProductoNoEncontradoException::new);
        if (!producto.isActivo()) {
            throw new ProductoNoEncontradoException();
        }
        return producto;
    }

    private BannerImagenPublicaDto toBannerImagenPublicaDto(TiendaBannerImagen imagen) {
        return new BannerImagenPublicaDto(imagen.getImagenUrl(), imagen.getProductoId());
    }

    // null si por alguna razón la categoría de tienda quedó apuntando a una
    // categoría que ya no existe (no debería pasar: no hay borrado de
    // Categoria hoy, pero el filtro defensivo evita un NPE si algún día lo
    // hay). Ver el .filter(Objects::nonNull) en listarCategorias.
    private CategoriaTiendaDto toCategoriaDto(TiendaCategoria tiendaCategoria, Categoria categoria) {
        if (categoria == null) {
            return null;
        }
        return new CategoriaTiendaDto(categoria.getId(), categoria.getNombre(), tiendaCategoria.getColor(), tiendaCategoria.getImagenUrl());
    }

    // Input del motor de descuento combo (ver CalculadorDescuentoComboService):
    // resuelve id + nombre de categoría/subcategoría del producto YA
    // PERSISTIDO, nunca de lo que mande el cliente. El nombre viaja solo
    // para armar el texto legible del descuento ("Mates + Bombillas"), el
    // matcheo real es por id (ver CalculadorDescuentoComboService.matchea).
    private LineaCarritoCombo toLineaCarritoCombo(Producto producto, int cantidad, BigDecimal precioUnitario) {
        Categoria categoria = producto.getCategoria();
        Subcategoria subcategoria = producto.getSubcategoria();
        return new LineaCarritoCombo(
                producto.getId(),
                cantidad,
                precioUnitario,
                categoria.getId(),
                categoria.getNombre(),
                subcategoria != null ? subcategoria.getId() : null,
                subcategoria != null ? subcategoria.getNombre() : null
        );
    }

    private PublicProductoDto toProductoDto(Producto producto, Resena resenaMasReciente) {
        ResenaDestacadaDto resenaDestacada = resenaMasReciente != null
                ? new ResenaDestacadaDto(resenaMasReciente.getClienteNombre(), resenaMasReciente.getComentario())
                : null;
        return new PublicProductoDto(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getCategoria().getNombre(),
                producto.getPrecioVenta(),
                producto.getFotoUrl(),
                producto.getFotoUrl2(),
                producto.getFotoUrl3(),
                stockDisponibleCalculator.calcular(producto),
                resenaDestacada
        );
    }
}
