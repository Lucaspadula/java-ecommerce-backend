package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarProductoRequest;
import com.sistventas.backend.dto.AjustePrecioCategoriaRequest;
import com.sistventas.backend.dto.AjustePrecioCategoriaResultadoDto;
import com.sistventas.backend.dto.CrearResenaRequest;
import com.sistventas.backend.dto.ProductoDto;
import com.sistventas.backend.dto.ProductoInsumoDto;
import com.sistventas.backend.dto.ProductoInsumoRequest;
import com.sistventas.backend.dto.ProductoRequest;
import com.sistventas.backend.dto.ResenaDto;
import com.sistventas.backend.dto.TipoAjustePrecio;
import com.sistventas.backend.entity.Categoria;
import com.sistventas.backend.entity.Insumo;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ProductoInsumo;
import com.sistventas.backend.entity.Resena;
import com.sistventas.backend.entity.Subcategoria;
import com.sistventas.backend.exception.ArchivoInvalidoException;
import com.sistventas.backend.exception.CategoriaNoEncontradaException;
import com.sistventas.backend.exception.InsumoNoEncontradoException;
import com.sistventas.backend.exception.ProductoNoEncontradoException;
import com.sistventas.backend.exception.ResenaNoEncontradaException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.exception.SubcategoriaNoEncontradaException;
import com.sistventas.backend.repository.CategoriaRepository;
import com.sistventas.backend.repository.InsumoRepository;
import com.sistventas.backend.repository.ProductoRepository;
import com.sistventas.backend.repository.ResenaRepository;
import com.sistventas.backend.repository.SubcategoriaRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.ProductoService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ProductoServiceImpl implements ProductoService {

    private static final Path UPLOAD_DIR = Paths.get("uploads", "productos");

    // Directorio separado del de productos: la foto de una reseña es un
    // respaldo visual del testimonio (ej. captura de WhatsApp), no una foto
    // de producto — se guarda aparte para no mezclar ambos ciclos de vida.
    private static final Path UPLOAD_DIR_RESENAS = Paths.get("uploads", "resenas");

    private final ProductoRepository productoRepository;
    private final InsumoRepository insumoRepository;
    private final ResenaRepository resenaRepository;
    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaRepository subcategoriaRepository;
    private final StockDisponibleCalculator stockDisponibleCalculator;
    private final ImagenUploadValidator imagenUploadValidator;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                                InsumoRepository insumoRepository,
                                ResenaRepository resenaRepository,
                                CategoriaRepository categoriaRepository,
                                SubcategoriaRepository subcategoriaRepository,
                                StockDisponibleCalculator stockDisponibleCalculator,
                                ImagenUploadValidator imagenUploadValidator) {
        this.productoRepository = productoRepository;
        this.insumoRepository = insumoRepository;
        this.resenaRepository = resenaRepository;
        this.categoriaRepository = categoriaRepository;
        this.subcategoriaRepository = subcategoriaRepository;
        this.stockDisponibleCalculator = stockDisponibleCalculator;
        this.imagenUploadValidator = imagenUploadValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDto> listar(UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        return productoRepository.findByEmpresaIdAndActivoTrue(empresaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoDto obtener(Long id, UserPrincipal principal) {
        return toDto(buscarPorEmpresa(id, principal));
    }

    @Override
    @Transactional
    public ProductoDto crear(ProductoRequest request, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);

        Producto producto = new Producto();
        producto.setEmpresaId(empresaId);
        producto.setActivo(true);
        producto.setFechaAlta(LocalDateTime.now());
        aplicarDatosComunes(producto, request.nombre(), request.categoriaId(), request.subcategoriaId(),
                request.descripcion(), request.precioVenta(), request.precioPorMayor(),
                request.cantidadMinimaMayorista(), request.insumos(), empresaId);
        // Sin stock inicial a mano: todo producto se compone de insumos, así
        // que su disponible sale siempre de StockDisponibleCalculator en
        // base al stock de esos insumos.

        return toDto(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoDto actualizar(Long id, ActualizarProductoRequest request, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Producto producto = buscarPorEmpresa(id, principal);
        aplicarDatosComunes(producto, request.nombre(), request.categoriaId(), request.subcategoriaId(),
                request.descripcion(), request.precioVenta(), request.precioPorMayor(),
                request.cantidadMinimaMayorista(), request.insumos(), empresaId);
        return toDto(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public void eliminar(Long id, UserPrincipal principal) {
        Producto producto = buscarPorEmpresa(id, principal);
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    @Override
    @Transactional
    public ProductoDto actualizarFoto(Long id, MultipartFile file, int slot, UserPrincipal principal) {
        validarSlot(slot);
        Producto producto = buscarPorEmpresa(id, principal);
        String extension = imagenUploadValidator.validarYObtenerExtension(file);
        String nombreArchivo = producto.getId() + "-" + UUID.randomUUID() + extension;

        try {
            Files.createDirectories(UPLOAD_DIR);
            Path destino = UPLOAD_DIR.resolve(nombreArchivo);
            file.transferTo(destino);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la imagen", ex);
        }

        setFotoSlot(producto, slot, "/uploads/productos/" + nombreArchivo);
        return toDto(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoDto eliminarFoto(Long id, int slot, UserPrincipal principal) {
        validarSlot(slot);
        Producto producto = buscarPorEmpresa(id, principal);
        borrarArchivoSiExiste(getFotoSlot(producto, slot));
        setFotoSlot(producto, slot, null);
        return toDto(productoRepository.save(producto));
    }

    private void validarSlot(int slot) {
        if (slot < 1 || slot > 3) {
            throw new ArchivoInvalidoException("El slot debe ser 1, 2 o 3");
        }
    }

    // Único punto que traduce slot -> campo de Producto, tanto para leer
    // (eliminarFoto necesita la URL vieja antes de pisarla) como para
    // escribir (actualizarFoto).
    private String getFotoSlot(Producto producto, int slot) {
        return switch (slot) {
            case 1 -> producto.getFotoUrl();
            case 2 -> producto.getFotoUrl2();
            default -> producto.getFotoUrl3();
        };
    }

    private void setFotoSlot(Producto producto, int slot, String url) {
        switch (slot) {
            case 1 -> producto.setFotoUrl(url);
            case 2 -> producto.setFotoUrl2(url);
            default -> producto.setFotoUrl3(url);
        }
    }

    // Best-effort: mismo criterio que PerfilServiceImpl/TiendaCategoriaServiceImpl
    // para borrar el archivo viejo del disco. Si falla (ej. ya no existe) no
    // bloquea la actualización del campo en la fila.
    private void borrarArchivoSiExiste(String fotoUrl) {
        if (fotoUrl == null) {
            return;
        }
        String prefijo = "/uploads/productos/";
        if (!fotoUrl.startsWith(prefijo)) {
            return;
        }
        try {
            Files.deleteIfExists(UPLOAD_DIR.resolve(fotoUrl.substring(prefijo.length())));
        } catch (IOException ignored) {
            // Best-effort: no bloquea la actualización de la metadata.
        }
    }

    @Override
    @Transactional
    public AjustePrecioCategoriaResultadoDto ajustarPrecioPorCategoria(
            AjustePrecioCategoriaRequest request, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        List<Producto> productos = productoRepository
                .findByEmpresaIdAndCategoriaIdAndActivoTrue(empresaId, request.categoriaId());

        for (Producto producto : productos) {
            producto.setPrecioVenta(calcularNuevoPrecio(producto.getPrecioVenta(), request.tipoAjuste(), request.valor()));
        }
        productoRepository.saveAll(productos);

        return new AjustePrecioCategoriaResultadoDto(productos.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResenaDto> listarResenas(Long productoId, UserPrincipal principal) {
        buscarPorEmpresa(productoId, principal);
        return resenaRepository.findByProductoIdOrderByFechaDesc(productoId).stream()
                .map(this::toResenaDto)
                .toList();
    }

    @Override
    @Transactional
    public ResenaDto crearResena(Long productoId, CrearResenaRequest request, UserPrincipal principal) {
        Producto producto = buscarPorEmpresa(productoId, principal);

        Resena resena = new Resena();
        resena.setProductoId(producto.getId());
        resena.setEmpresaId(producto.getEmpresaId());
        resena.setClienteNombre(request.clienteNombre());
        resena.setComentario(request.comentario());
        resena.setFecha(LocalDateTime.now());

        return toResenaDto(resenaRepository.save(resena));
    }

    @Override
    @Transactional
    public void eliminarResena(Long productoId, Long resenaId, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        buscarPorEmpresa(productoId, principal);

        Resena resena = resenaRepository.findByIdAndProductoIdAndEmpresaId(resenaId, productoId, empresaId)
                .orElseThrow(ResenaNoEncontradaException::new);
        resenaRepository.delete(resena);
    }

    @Override
    @Transactional
    public ResenaDto actualizarFotoResena(Long productoId, Long resenaId, MultipartFile file, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        buscarPorEmpresa(productoId, principal);

        // Mismo criterio multi-tenant que eliminarResena: la reseña tiene
        // que pertenecer a ESE producto Y a la empresa del usuario, no solo
        // el id de la reseña suelto.
        Resena resena = resenaRepository.findByIdAndProductoIdAndEmpresaId(resenaId, productoId, empresaId)
                .orElseThrow(ResenaNoEncontradaException::new);

        String extension = imagenUploadValidator.validarYObtenerExtension(file);
        String nombreArchivo = "resena-" + resena.getId() + "-" + UUID.randomUUID() + extension;

        try {
            Files.createDirectories(UPLOAD_DIR_RESENAS);
            Path destino = UPLOAD_DIR_RESENAS.resolve(nombreArchivo);
            file.transferTo(destino);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la imagen", ex);
        }

        resena.setImagenUrl("/uploads/resenas/" + nombreArchivo);
        return toResenaDto(resenaRepository.save(resena));
    }

    private ResenaDto toResenaDto(Resena resena) {
        return new ResenaDto(resena.getId(), resena.getClienteNombre(), resena.getComentario(), resena.getFecha(),
                resena.getImagenUrl());
    }

    // Nunca deja el precio negativo: un ajuste porcentual/monto fijo mal
    // cargado (ej. -200%) se clampea a 0 en vez de dejar un precio inválido.
    private BigDecimal calcularNuevoPrecio(BigDecimal precioActual, TipoAjustePrecio tipo, BigDecimal valor) {
        BigDecimal nuevoPrecio = tipo == TipoAjustePrecio.PORCENTAJE
                ? precioActual.add(precioActual.multiply(valor).divide(BigDecimal.valueOf(100)))
                : precioActual.add(valor);
        return nuevoPrecio.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    // Campos compartidos entre alta y edición. El stock queda deliberadamente
    // afuera: nunca se carga a mano, ni siquiera al crear — sale siempre del
    // stock de los insumos de la receta (ver StockDisponibleCalculator).
    private void aplicarDatosComunes(Producto producto, String nombre, Long categoriaId, Long subcategoriaId,
                                      String descripcion, BigDecimal precioVenta, BigDecimal precioPorMayor,
                                      Integer cantidadMinimaMayorista,
                                      List<ProductoInsumoRequest> insumosRequest, Long empresaId) {
        producto.setNombre(nombre);
        producto.setCategoria(buscarCategoriaPorEmpresa(categoriaId, empresaId));
        producto.setSubcategoria(subcategoriaId != null
                ? buscarSubcategoriaDeCategoria(subcategoriaId, categoriaId)
                : null);
        producto.setDescripcion(descripcion);
        producto.setPrecioVenta(precioVenta);
        producto.setPrecioPorMayor(precioPorMayor);
        producto.setCantidadMinimaMayorista(cantidadMinimaMayorista);

        // Reemplazo completo de la lista: más simple que un diff y suficiente
        // para el caso de uso (el frontend siempre manda la receta entera).
        // orphanRemoval=true en Producto.insumos borra las filas viejas al
        // hacer flush. La lista nunca llega vacía acá: @NotEmpty en
        // ProductoRequest/ActualizarProductoRequest ya lo garantiza antes de
        // que el controller invoque el service (ver @Valid en
        // ProductoController).
        producto.getInsumos().clear();
        for (ProductoInsumoRequest insumoRequest : insumosRequest) {
            Insumo insumoMaestro = buscarInsumoPorEmpresa(insumoRequest.insumoId(), empresaId);
            ProductoInsumo productoInsumo = new ProductoInsumo();
            productoInsumo.setProducto(producto);
            productoInsumo.setInsumo(insumoMaestro);
            productoInsumo.setCantidad(insumoRequest.cantidad());
            producto.getInsumos().add(productoInsumo);
        }
    }

    private Insumo buscarInsumoPorEmpresa(Long insumoId, Long empresaId) {
        return insumoRepository.findByIdAndEmpresaId(insumoId, empresaId)
                .orElseThrow(InsumoNoEncontradoException::new);
    }

    private Categoria buscarCategoriaPorEmpresa(Long categoriaId, Long empresaId) {
        return categoriaRepository.findByIdAndEmpresaId(categoriaId, empresaId)
                .orElseThrow(CategoriaNoEncontradaException::new);
    }

    // Valida que la subcategoría sea hija de LA categoría elegida en esta
    // misma request (no alcanza con que exista, evita mezclar la
    // subcategoría de una categoría con otra distinta por un id suelto).
    private Subcategoria buscarSubcategoriaDeCategoria(Long subcategoriaId, Long categoriaId) {
        return subcategoriaRepository.findByIdAndCategoriaId(subcategoriaId, categoriaId)
                .orElseThrow(SubcategoriaNoEncontradaException::new);
    }

    private Producto buscarPorEmpresa(Long id, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        return productoRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(ProductoNoEncontradoException::new);
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

    private ProductoDto toDto(Producto producto) {
        List<ProductoInsumoDto> insumos = producto.getInsumos().stream()
                .map(this::toProductoInsumoDto)
                .toList();

        return new ProductoDto(
                producto.getId(),
                producto.getNombre(),
                // Resuelto acá, no expuesto como id: el DTO le sigue
                // hablando al frontend en nombres (ver decisión documentada
                // en ProductoDto), la normalización a tabla maestra es
                // interna.
                producto.getCategoria().getNombre(),
                producto.getSubcategoria() != null ? producto.getSubcategoria().getNombre() : null,
                producto.getDescripcion(),
                producto.getPrecioVenta(),
                producto.getPrecioPorMayor(),
                producto.getCantidadMinimaMayorista(),
                producto.getFotoUrl(),
                producto.getFotoUrl2(),
                producto.getFotoUrl3(),
                // Calculado vía StockDisponibleCalculator (compartido con
                // PublicTiendaServiceImpl): así todo el sistema ve siempre el
                // mismo número sin saber si es un valor cargado a mano o
                // calculado a partir de insumos. Ver StockDisponibleCalculator
                // para el criterio completo.
                stockDisponibleCalculator.calcular(producto),
                calcularCostoUnitario(insumos),
                insumos
        );
    }

    // Todo producto se compone de insumos, así que el costo es siempre la
    // suma del subtotal de cada línea de la receta — mismo cálculo que antes
    // hacía el frontend a mano.
    private BigDecimal calcularCostoUnitario(List<ProductoInsumoDto> insumos) {
        return insumos.stream()
                .map(ProductoInsumoDto::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ProductoInsumoDto toProductoInsumoDto(ProductoInsumo productoInsumo) {
        Insumo insumo = productoInsumo.getInsumo();
        BigDecimal costoUnitario = insumo.getCostoUnitario();
        BigDecimal subtotal = costoUnitario.multiply(productoInsumo.getCantidad());
        return new ProductoInsumoDto(
                productoInsumo.getId(),
                insumo.getId(),
                insumo.getNombre(),
                productoInsumo.getCantidad(),
                costoUnitario,
                subtotal
        );
    }
}
