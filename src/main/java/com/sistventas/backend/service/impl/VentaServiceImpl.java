package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarEstadoVentaRequest;
import com.sistventas.backend.dto.FotoUploadDto;
import com.sistventas.backend.dto.TextoCompartirDto;
import com.sistventas.backend.dto.VentaDto;
import com.sistventas.backend.dto.VentaItemDto;
import com.sistventas.backend.dto.VentaItemRequest;
import com.sistventas.backend.dto.VentaEstadoHistorialDto;
import com.sistventas.backend.dto.VentaRequest;
import com.sistventas.backend.entity.Cliente;
import com.sistventas.backend.entity.EstadoVenta;
import com.sistventas.backend.entity.Insumo;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ProductoInsumo;
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
import com.sistventas.backend.repository.UsuarioRepository;
import com.sistventas.backend.repository.VentaEstadoHistorialRepository;
import com.sistventas.backend.repository.VentaRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.VentaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class VentaServiceImpl implements VentaService {

    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Path UPLOAD_DIR = Paths.get("uploads", "ventas");

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final InsumoRepository insumoRepository;
    private final VentaEstadoHistorialRepository ventaEstadoHistorialRepository;
    private final UsuarioRepository usuarioRepository;
    private final ImagenUploadValidator imagenUploadValidator;

    public VentaServiceImpl(VentaRepository ventaRepository,
                             ClienteRepository clienteRepository,
                             ProductoRepository productoRepository,
                             InsumoRepository insumoRepository,
                             VentaEstadoHistorialRepository ventaEstadoHistorialRepository,
                             UsuarioRepository usuarioRepository,
                             ImagenUploadValidator imagenUploadValidator) {
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.insumoRepository = insumoRepository;
        this.ventaEstadoHistorialRepository = ventaEstadoHistorialRepository;
        this.usuarioRepository = usuarioRepository;
        this.imagenUploadValidator = imagenUploadValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaDto> listar(EstadoVenta estado, Long clienteId, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);

        List<Venta> ventas;
        if (clienteId != null) {
            // Historial de compras de un cliente puntual: todas sus ventas,
            // incluidas las canceladas (es un historial, no un pipeline).
            ventas = ventaRepository.findByEmpresaIdAndClienteIdOrderByFechaPedidoDesc(empresaId, clienteId);
        } else if (estado != null) {
            ventas = ventaRepository.findByEmpresaIdAndEstado(empresaId, estado);
        } else {
            ventas = ventaRepository.findByEmpresaIdAndEstadoNot(empresaId, EstadoVenta.CANCELADA);
        }

        // Batch fetch de nombres de cliente para evitar N+1: una sola query
        // por los ids distintos en vez de una consulta por cada venta.
        List<Long> clienteIds = ventas.stream().map(Venta::getClienteId).distinct().toList();
        Map<Long, String> nombresPorCliente = clienteRepository.findAllById(clienteIds).stream()
                .collect(java.util.stream.Collectors.toMap(Cliente::getId, Cliente::getNombre));

        return ventas.stream()
                .map(venta -> toDto(venta, nombresPorCliente.get(venta.getClienteId())))
                // Sin fecha de entrega van al final; el resto ordenado ascendente.
                .sorted(Comparator.comparing(VentaDto::fechaEntrega, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaEstadoHistorialDto> historialEstados(Long id, UserPrincipal principal) {
        // Valida que la venta pertenezca a la empresa del usuario (scoping),
        // aunque no se use el resultado más que para eso.
        buscarPorEmpresa(id, principal);
        return ventaEstadoHistorialRepository.findByVentaIdOrderByFechaDescIdDesc(id).stream()
                .map(this::toHistorialDto)
                .toList();
    }

    private VentaEstadoHistorialDto toHistorialDto(VentaEstadoHistorial historial) {
        String usuarioNombre = usuarioRepository.findById(historial.getUsuarioId())
                .map(Usuario::getNombre)
                .orElse("Usuario eliminado");
        return new VentaEstadoHistorialDto(
                historial.getId(),
                historial.getEstadoAnterior(),
                historial.getEstadoNuevo(),
                usuarioNombre,
                historial.getFecha()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public VentaDto obtener(Long id, UserPrincipal principal) {
        Venta venta = buscarPorEmpresa(id, principal);
        return toDto(venta, nombreCliente(venta.getClienteId(), principal));
    }

    @Override
    @Transactional
    public VentaDto crear(VentaRequest request, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Cliente cliente = buscarClientePorEmpresa(request.clienteId(), empresaId);

        Venta venta = new Venta();
        venta.setEmpresaId(empresaId);
        venta.setEstado(EstadoVenta.PRESUPUESTO);
        venta.setFechaPedido(LocalDateTime.now());
        venta.setFechaAlta(LocalDateTime.now());
        aplicarDatos(venta, request, empresaId);

        return toDto(ventaRepository.save(venta), cliente.getNombre());
    }

    @Override
    @Transactional
    public VentaDto actualizar(Long id, VentaRequest request, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Venta venta = buscarPorEmpresa(id, principal);
        Cliente cliente = buscarClientePorEmpresa(request.clienteId(), empresaId);

        // Si la venta ya tenía stock reservado (se había confirmado en algún
        // punto y no está cancelada), hay que restaurar el stock de los items
        // VIEJOS antes de que aplicarDatos los reemplace, y volver a
        // descontar con los items NUEVOS. Si estaba en PRESUPUESTO no hay
        // nada reservado, así que la edición no toca stock.
        boolean teniaStockReservado = holdsStock(venta.getEstado());
        if (teniaStockReservado) {
            restaurarStock(venta, empresaId);
        }

        aplicarDatos(venta, request, empresaId);

        if (teniaStockReservado) {
            descontarStock(venta, empresaId);
        }

        return toDto(ventaRepository.save(venta), cliente.getNombre());
    }

    @Override
    @Transactional
    public VentaDto actualizarEstado(Long id, ActualizarEstadoVentaRequest request, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Venta venta = buscarPorEmpresa(id, principal);
        EstadoVenta estadoAnterior = venta.getEstado();
        EstadoVenta estadoNuevo = request.estado();

        if (estadoNuevo == EstadoVenta.CANCELADA) {
            cancelar(venta, empresaId);
        } else {
            // Un presupuesto no reserva stock: recién se descuenta cuando
            // pasa de PRESUPUESTO (o de una CANCELADA reactivada) a un
            // estado "confirmado". Entre estados ya confirmados
            // (CONFIRMADA -> EN_PROCESO -> LISTA -> ENTREGADA) no se toca
            // stock, ya se descontó una sola vez al confirmar.
            if (!holdsStock(venta.getEstado()) && holdsStock(estadoNuevo)) {
                descontarStock(venta, empresaId);
            }
            venta.setEstado(estadoNuevo);
        }

        registrarCambioEstado(venta, estadoAnterior, principal);
        Venta guardada = ventaRepository.save(venta);
        return toDto(guardada, nombreCliente(guardada.getClienteId(), principal));
    }

    @Override
    @Transactional
    public void eliminar(Long id, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Venta venta = buscarPorEmpresa(id, principal);
        EstadoVenta estadoAnterior = venta.getEstado();
        // "Eliminar" una venta en realidad es cancelarla: no se borra la
        // fila (ver comentario histórico), mismo camino que
        // actualizarEstado(CANCELADA).
        cancelar(venta, empresaId);
        registrarCambioEstado(venta, estadoAnterior, principal);
        ventaRepository.save(venta);
    }

    // Registra en el historial de auditoría solo si el estado efectivamente
    // cambió (ej. cancelar una venta ya CANCELADA no genera una fila nueva).
    private void registrarCambioEstado(Venta venta, EstadoVenta estadoAnterior, UserPrincipal principal) {
        if (estadoAnterior == venta.getEstado()) {
            return;
        }
        VentaEstadoHistorial historial = new VentaEstadoHistorial();
        historial.setVentaId(venta.getId());
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(venta.getEstado());
        historial.setUsuarioId(principal.id());
        historial.setFecha(LocalDateTime.now());
        ventaEstadoHistorialRepository.save(historial);
    }

    // Cancela una venta: si su estado actual tenía stock reservado, lo
    // restaura antes de marcarla CANCELADA. Compartido por eliminar() y por
    // actualizarEstado() cuando el nuevo estado es CANCELADA.
    private void cancelar(Venta venta, Long empresaId) {
        if (holdsStock(venta.getEstado())) {
            restaurarStock(venta, empresaId);
        }
        venta.setEstado(EstadoVenta.CANCELADA);
    }

    // true si el estado implica que el pedido ya tiene stock reservado: fue
    // confirmado en algún momento y todavía no se canceló. PRESUPUESTO y
    // CANCELADA nunca tienen stock reservado.
    private boolean holdsStock(EstadoVenta estado) {
        return estado != EstadoVenta.PRESUPUESTO && estado != EstadoVenta.CANCELADA;
    }

    // Descuenta stock de todos los items de la venta. Agrupa por producto
    // ANTES de validar/aplicar: si dos líneas de la misma venta apuntan al
    // mismo producto (ej. dos personalizaciones distintas del mismo
    // producto), validar/descontar línea por línea contra el stock original
    // dejaría pasar una demanda combinada mayor al stock disponible.
    //
    // Desde que existen productos "con receta" (se arman al momento a partir
    // de insumos sueltos, no tienen unidades ya armadas en stock propio), el
    // descuento ya no es siempre "restar en Producto.stock":
    //   - Producto SIN receta (insumos vacío): comportamiento de siempre,
    //     descuenta Producto.stock.
    //   - Producto CON receta: no tiene stock propio que mover. En su lugar
    //     se descuenta, por cada línea de su receta, insumo.stock -=
    //     (cantidad de la receta × cantidad vendida).
    // Primero valida en un loop separado que TODO alcance (productos sin
    // receta contra su stock propio, e insumos consumidos por productos con
    // receta contra el stock de cada insumo) antes de aplicar ningún cambio:
    // si algo falla, no queremos dejar descuentos parciales aplicados (la
    // excepción además aborta la transacción completa por el
    // @Transactional del método que llama a este).
    private void descontarStock(Venta venta, Long empresaId) {
        Map<Long, Integer> cantidadPorProducto = cantidadPorProducto(venta);
        Map<Long, Producto> productos = resolverProductos(cantidadPorProducto.keySet(), empresaId);

        validarStockSuficiente(cantidadPorProducto, productos);

        for (Map.Entry<Long, Integer> entry : cantidadPorProducto.entrySet()) {
            Producto producto = productos.get(entry.getKey());
            if (producto.getInsumos().isEmpty()) {
                producto.setStock(producto.getStock() - entry.getValue());
                productoRepository.save(producto);
            }
        }
        aplicarConsumoInsumos(cantidadPorProducto, productos, false);
    }

    // Devuelve al stock los items de una venta que ya lo había descontado:
    // mismo criterio que descontarStock pero sumando en vez de restar, y sin
    // validación (devolver stock nunca puede dejarlo negativo).
    private void restaurarStock(Venta venta, Long empresaId) {
        Map<Long, Integer> cantidadPorProducto = cantidadPorProducto(venta);
        Map<Long, Producto> productos = resolverProductos(cantidadPorProducto.keySet(), empresaId);

        for (Map.Entry<Long, Integer> entry : cantidadPorProducto.entrySet()) {
            Producto producto = productos.get(entry.getKey());
            if (producto.getInsumos().isEmpty()) {
                producto.setStock(producto.getStock() + entry.getValue());
                productoRepository.save(producto);
            }
        }
        aplicarConsumoInsumos(cantidadPorProducto, productos, true);
    }

    // Valida que haya stock suficiente para la demanda total de la venta.
    // Dos chequeos separados:
    //   1) Productos SIN receta: contra su propio Producto.stock (igual que
    //      siempre).
    //   2) Insumos consumidos por productos CON receta: se agrega el
    //      consumo total de CADA insumo across TODAS las líneas de receta de
    //      TODOS los productos con receta de la venta (consumoPorInsumo)
    //      antes de comparar contra el stock del insumo. Esto es a propósito
    //      así: si dos productos distintos de la misma venta comparten un
    //      insumo (ej. el mismo tipo de bombilla en dos mates distintos), la
    //      demanda combinada se valida junta — validar cada producto por
    //      separado contra el stock "actual" del insumo dejaría pasar un
    //      caso donde ninguno individualmente supera el stock pero la suma
    //      de ambos sí.
    private void validarStockSuficiente(Map<Long, Integer> cantidadPorProducto, Map<Long, Producto> productos) {
        for (Map.Entry<Long, Integer> entry : cantidadPorProducto.entrySet()) {
            Producto producto = productos.get(entry.getKey());
            if (producto.getInsumos().isEmpty() && producto.getStock() < entry.getValue()) {
                throw new StockInsuficienteException(producto.getNombre(), producto.getStock(), entry.getValue());
            }
        }

        Map<Long, BigDecimal> consumoPorInsumo = consumoPorInsumo(cantidadPorProducto, productos);
        if (consumoPorInsumo.isEmpty()) {
            return;
        }
        for (Insumo insumo : insumoRepository.findAllById(consumoPorInsumo.keySet())) {
            BigDecimal solicitado = consumoPorInsumo.get(insumo.getId());
            if (insumo.getStock().compareTo(solicitado) < 0) {
                throw new StockInsuficienteException(insumo.getNombre(), insumo.getStock().intValue(), solicitado.intValue());
            }
        }
    }

    // Aplica (descuenta o devuelve) el consumo de insumos agregado de la
    // venta. Resuelve los insumos afectados en una sola pasada
    // (findAllById) y aplica el delta una única vez por insumo — así, si el
    // mismo insumo aparece en la receta de más de un producto de la venta,
    // el ajuste de stock se hace sobre el consumo YA SUMADO en vez de
    // arrastrar guardados parciales/duplicados por línea.
    private void aplicarConsumoInsumos(Map<Long, Integer> cantidadPorProducto, Map<Long, Producto> productos, boolean devolver) {
        Map<Long, BigDecimal> consumoPorInsumo = consumoPorInsumo(cantidadPorProducto, productos);
        if (consumoPorInsumo.isEmpty()) {
            return;
        }
        for (Insumo insumo : insumoRepository.findAllById(consumoPorInsumo.keySet())) {
            BigDecimal consumo = consumoPorInsumo.get(insumo.getId());
            insumo.setStock(devolver ? insumo.getStock().add(consumo) : insumo.getStock().subtract(consumo));
            insumoRepository.save(insumo);
        }
    }

    // Suma, por insumoId, cuánto consume la venta de ese insumo: recorre
    // solo los productos CON receta de la venta y, por cada línea de su
    // receta, acumula (cantidad de la receta × cantidad vendida del
    // producto). Un mismo insumo puede aparecer en la receta de varios
    // productos de la misma venta (o más de una vez si el producto lo
    // repite) — merge() lo suma en vez de pisarlo.
    private Map<Long, BigDecimal> consumoPorInsumo(Map<Long, Integer> cantidadPorProducto, Map<Long, Producto> productos) {
        Map<Long, BigDecimal> consumo = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : cantidadPorProducto.entrySet()) {
            Producto producto = productos.get(entry.getKey());
            if (producto.getInsumos().isEmpty()) {
                continue;
            }
            BigDecimal cantidadVenta = BigDecimal.valueOf(entry.getValue());
            for (ProductoInsumo productoInsumo : producto.getInsumos()) {
                consumo.merge(productoInsumo.getInsumo().getId(),
                        productoInsumo.getCantidad().multiply(cantidadVenta), BigDecimal::add);
            }
        }
        return consumo;
    }

    // Resuelve, en un solo mapa, los productos referenciados por la venta
    // (uno por productoId, ya agrupados por cantidadPorProducto). Se usa
    // tanto por descontarStock como por restaurarStock para no repetir la
    // misma búsqueda dos veces (una para validar, otra para aplicar).
    private Map<Long, Producto> resolverProductos(Set<Long> productoIds, Long empresaId) {
        Map<Long, Producto> productos = new LinkedHashMap<>();
        for (Long productoId : productoIds) {
            productos.put(productoId, buscarProductoParaStock(productoId, empresaId));
        }
        return productos;
    }

    // Suma las cantidades de la venta por productoId, para tratar líneas
    // repetidas del mismo producto como una sola demanda combinada.
    private Map<Long, Integer> cantidadPorProducto(Venta venta) {
        return venta.getItems().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        VentaItem::getProductoId,
                        java.util.stream.Collectors.summingInt(VentaItem::getCantidad)));
    }

    // A diferencia de buscarProductoPorEmpresa (usado al armar los items de
    // una venta nueva/editada), esta búsqueda NO exige producto.isActivo():
    // un producto puede haberse dado de baja después de vendido, y la venta
    // ya existente tiene que poder seguir moviendo su stock (confirmar,
    // cancelar, restaurar) sin romperse por eso.
    private Producto buscarProductoParaStock(Long productoId, Long empresaId) {
        return productoRepository.findByIdAndEmpresaId(productoId, empresaId)
                .orElseThrow(ProductoNoEncontradoException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public TextoCompartirDto textoCompartir(Long id, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Venta venta = buscarPorEmpresa(id, principal);
        Cliente cliente = clienteRepository.findByIdAndEmpresaId(venta.getClienteId(), empresaId)
                .orElseThrow(ClienteNoEncontradoException::new);

        String texto = construirTextoCompartir(venta, cliente.getNombre());
        return new TextoCompartirDto(texto, cliente.getTelefono());
    }

    @Override
    public FotoUploadDto subirFoto(MultipartFile file, UserPrincipal principal) {
        // No persiste nada: solo valida acceso (empresaIdOrThrow) y escribe el
        // archivo a disco. La url resultante la asocia el cliente a un item
        // recién cuando manda el POST/PUT /api/ventas con esa fotoUrl.
        empresaIdOrThrow(principal);
        String extension = imagenUploadValidator.validarYObtenerExtension(file);
        String nombreArchivo = UUID.randomUUID() + extension;

        try {
            Files.createDirectories(UPLOAD_DIR);
            Path destino = UPLOAD_DIR.resolve(nombreArchivo);
            file.transferTo(destino);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la imagen", ex);
        }

        return new FotoUploadDto("/uploads/ventas/" + nombreArchivo);
    }

    private String construirTextoCompartir(Venta venta, String clienteNombre) {
        StringBuilder sb = new StringBuilder();
        sb.append("🧾 PRESUPUESTO #").append(venta.getId()).append(" — Sistema Ventas\n");
        sb.append("Cliente: ").append(clienteNombre).append("\n");
        sb.append("Fecha: ").append(venta.getFechaPedido().format(FECHA_FORMATTER)).append("\n");
        if (venta.getFechaEntrega() != null) {
            sb.append("Entrega estimada: ").append(venta.getFechaEntrega().format(FECHA_FORMATTER)).append("\n");
        }
        sb.append("--------------------------------\n");
        for (VentaItem item : venta.getItems()) {
            sb.append(item.getCantidad()).append("x ").append(item.getProductoNombre())
                    .append(" — $").append(formatMonto(item.getSubtotal())).append("\n");
            if (item.getPersonalizacion() != null && !item.getPersonalizacion().isBlank()) {
                sb.append("   ✎ ").append(item.getPersonalizacion()).append("\n");
            }
        }
        sb.append("--------------------------------\n");
        sb.append("Subtotal: $").append(formatMonto(venta.getSubtotal())).append("\n");
        if (venta.getDescuentoPorcentaje().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Descuento: ").append(venta.getDescuentoPorcentaje().stripTrailingZeros().toPlainString()).append("%\n");
        }
        sb.append("TOTAL: $").append(formatMonto(venta.getTotal())).append("\n");
        sb.append("\n¿Confirmás el pedido? 😊");
        return sb.toString();
    }

    // NumberFormat no es thread-safe: se instancia por llamada en vez de
    // guardarlo como campo estático compartido (el service es un singleton).
    private String formatMonto(BigDecimal monto) {
        NumberFormat formatter = NumberFormat.getIntegerInstance(new Locale("es", "AR"));
        return formatter.format(monto.setScale(0, RoundingMode.HALF_UP));
    }

    // Reemplazo completo de la lista de items: más simple que un diff y
    // suficiente para el caso de uso (el frontend siempre manda el pedido
    // entero). orphanRemoval=true en Venta.items borra las filas viejas al
    // hacer flush, mismo patrón que ProductoServiceImpl con los insumos.
    private void aplicarDatos(Venta venta, VentaRequest request, Long empresaId) {
        venta.setClienteId(request.clienteId());
        venta.setFechaEntrega(request.fechaEntrega());
        venta.setNotas(request.notas());

        venta.getItems().clear();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (VentaItemRequest itemRequest : request.items()) {
            Producto producto = buscarProductoPorEmpresa(itemRequest.productoId(), empresaId);

            BigDecimal precioUnitario = itemRequest.precioUnitario() != null
                    ? itemRequest.precioUnitario()
                    : precioSegunCantidad(producto, itemRequest.cantidad());
            BigDecimal subtotalItem = precioUnitario.multiply(BigDecimal.valueOf(itemRequest.cantidad()));

            VentaItem item = new VentaItem();
            item.setVenta(venta);
            item.setProductoId(producto.getId());
            item.setProductoNombre(producto.getNombre());
            item.setCantidad(itemRequest.cantidad());
            item.setPrecioUnitario(precioUnitario);
            item.setPersonalizacion(itemRequest.personalizacion());
            item.setSubtotal(subtotalItem);
            item.setFotoUrl(itemRequest.fotoUrl());
            venta.getItems().add(item);

            subtotal = subtotal.add(subtotalItem);
        }

        BigDecimal descuentoPorcentaje = request.descuentoPorcentaje() != null
                ? request.descuentoPorcentaje()
                : BigDecimal.ZERO;
        BigDecimal total = subtotal
                .multiply(BigDecimal.ONE.subtract(descuentoPorcentaje.divide(BigDecimal.valueOf(100))))
                .setScale(2, RoundingMode.HALF_UP);

        venta.setSubtotal(subtotal);
        venta.setDescuentoPorcentaje(descuentoPorcentaje);
        venta.setTotal(total);
    }

    // Si el producto tiene precio por mayor Y cantidad mínima cargados, y la
    // cantidad pedida llega a ese mínimo, usa el precio mayorista. Si no,
    // el precio normal. Solo se usa cuando el cliente no manda un
    // precioUnitario explícito (el front lo puede seguir pisando a mano).
    private BigDecimal precioSegunCantidad(Producto producto, int cantidad) {
        if (producto.getPrecioPorMayor() != null
                && producto.getCantidadMinimaMayorista() != null
                && cantidad >= producto.getCantidadMinimaMayorista()) {
            return producto.getPrecioPorMayor();
        }
        return producto.getPrecioVenta();
    }

    private Venta buscarPorEmpresa(Long id, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        return ventaRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(VentaNoEncontradaException::new);
    }

    private Cliente buscarClientePorEmpresa(Long clienteId, Long empresaId) {
        return clienteRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(ClienteNoEncontradoException::new);
    }

    private Producto buscarProductoPorEmpresa(Long productoId, Long empresaId) {
        Producto producto = productoRepository.findByIdAndEmpresaId(productoId, empresaId)
                .orElseThrow(ProductoNoEncontradoException::new);
        if (!producto.isActivo()) {
            throw new ProductoNoEncontradoException();
        }
        return producto;
    }

    private String nombreCliente(Long clienteId, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        return clienteRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .map(Cliente::getNombre)
                .orElse(null);
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

    private VentaDto toDto(Venta venta, String clienteNombre) {
        List<VentaItemDto> items = venta.getItems().stream()
                .map(this::toItemDto)
                .toList();

        return new VentaDto(
                venta.getId(),
                venta.getClienteId(),
                clienteNombre,
                venta.getEstado(),
                venta.getFechaPedido(),
                venta.getFechaEntrega(),
                venta.getNotas(),
                venta.getTotal(),
                venta.getSubtotal(),
                venta.getDescuentoPorcentaje(),
                items,
                venta.getFechaAlta(),
                venta.getDireccionEnvio(),
                venta.getLocalidad(),
                venta.getCodigoPostal(),
                venta.getDescuentoComboMonto(),
                venta.getDescuentoComboDetalle()
        );
    }

    private VentaItemDto toItemDto(VentaItem item) {
        return new VentaItemDto(
                item.getId(),
                item.getProductoId(),
                item.getProductoNombre(),
                item.getCantidad(),
                item.getPrecioUnitario(),
                item.getPersonalizacion(),
                item.getSubtotal(),
                item.getFotoUrl()
        );
    }
}
