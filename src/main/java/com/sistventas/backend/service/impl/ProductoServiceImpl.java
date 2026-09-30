package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarProductoRequest;
import com.sistventas.backend.dto.AjustePrecioCategoriaRequest;
import com.sistventas.backend.dto.AjustePrecioCategoriaResultadoDto;
import com.sistventas.backend.dto.AtributoFiltroValorDto;
import com.sistventas.backend.dto.CrearResenaRequest;
import com.sistventas.backend.dto.FotoUploadDto;
import com.sistventas.backend.dto.ProductoComponenteDto;
import com.sistventas.backend.dto.ProductoComponenteRequest;
import com.sistventas.backend.dto.ProductoDto;
import com.sistventas.backend.dto.ProductoFotoDto;
import com.sistventas.backend.dto.ProductoGrabadoDto;
import com.sistventas.backend.dto.ProductoGrabadoRequest;
import com.sistventas.backend.dto.ProductoInsumoDto;
import com.sistventas.backend.dto.ProductoInsumoRequest;
import com.sistventas.backend.dto.ProductoRequest;
import com.sistventas.backend.dto.ProductoVarianteDto;
import com.sistventas.backend.dto.ProductoVarianteRequest;
import com.sistventas.backend.dto.ResenaDto;
import com.sistventas.backend.dto.TipoAjustePrecio;
import com.sistventas.backend.entity.AtributoFiltro;
import com.sistventas.backend.entity.AtributoFiltroValor;
import com.sistventas.backend.entity.Categoria;
import com.sistventas.backend.entity.Insumo;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ProductoComponente;
import com.sistventas.backend.entity.ProductoFoto;
import com.sistventas.backend.entity.ProductoGrabado;
import com.sistventas.backend.entity.ProductoInsumo;
import com.sistventas.backend.entity.ProductoVariante;
import com.sistventas.backend.entity.Resena;
import com.sistventas.backend.entity.Subcategoria;
import com.sistventas.backend.exception.ArchivoInvalidoException;
import com.sistventas.backend.exception.AtributoFiltroValorNoEncontradoException;
import com.sistventas.backend.exception.CategoriaNoEncontradaException;
import com.sistventas.backend.exception.InsumoNoEncontradoException;
import com.sistventas.backend.exception.ProductoComponenteInvalidoException;
import com.sistventas.backend.exception.ProductoFotoNoEncontradaException;
import com.sistventas.backend.exception.ProductoNoEncontradoException;
import com.sistventas.backend.exception.ResenaNoEncontradaException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.exception.SubcategoriaNoEncontradaException;
import com.sistventas.backend.repository.AtributoFiltroRepository;
import com.sistventas.backend.repository.AtributoFiltroValorRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final AtributoFiltroRepository atributoFiltroRepository;
    private final AtributoFiltroValorRepository atributoFiltroValorRepository;
    private final StockDisponibleCalculator stockDisponibleCalculator;
    private final ImagenUploadValidator imagenUploadValidator;
    private final ProductoFotoResolver productoFotoResolver;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                                InsumoRepository insumoRepository,
                                ResenaRepository resenaRepository,
                                CategoriaRepository categoriaRepository,
                                SubcategoriaRepository subcategoriaRepository,
                                AtributoFiltroRepository atributoFiltroRepository,
                                AtributoFiltroValorRepository atributoFiltroValorRepository,
                                StockDisponibleCalculator stockDisponibleCalculator,
                                ImagenUploadValidator imagenUploadValidator,
                                ProductoFotoResolver productoFotoResolver) {
        this.productoRepository = productoRepository;
        this.insumoRepository = insumoRepository;
        this.resenaRepository = resenaRepository;
        this.categoriaRepository = categoriaRepository;
        this.subcategoriaRepository = subcategoriaRepository;
        this.atributoFiltroRepository = atributoFiltroRepository;
        this.atributoFiltroValorRepository = atributoFiltroValorRepository;
        this.stockDisponibleCalculator = stockDisponibleCalculator;
        this.imagenUploadValidator = imagenUploadValidator;
        this.productoFotoResolver = productoFotoResolver;
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
                request.cantidadMinimaMayorista(), request.insumos(), request.variantes(),
                request.componentes(), request.grabados(), request.stock(), request.costoUnitario(), empresaId,
                request.atributoValorIds());

        return toDto(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoDto actualizar(Long id, ActualizarProductoRequest request, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Producto producto = buscarPorEmpresa(id, principal);
        aplicarDatosComunes(producto, request.nombre(), request.categoriaId(), request.subcategoriaId(),
                request.descripcion(), request.precioVenta(), request.precioPorMayor(),
                request.cantidadMinimaMayorista(), request.insumos(), request.variantes(),
                request.componentes(), request.grabados(), request.stock(), request.costoUnitario(), empresaId,
                request.atributoValorIds());
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
    public ProductoDto agregarFoto(Long id, MultipartFile file, Long varianteId, UserPrincipal principal) {
        Producto producto = buscarPorEmpresa(id, principal);

        // Tope 8 validado ANTES de escribir nada a disco (ver design Open
        // Question): un intento rechazado no debe dejar un archivo huérfano.
        if (producto.getFotos().size() >= 8) {
            throw new ArchivoInvalidoException("Máximo 8 fotos por producto");
        }
        if (varianteId != null) {
            buscarVarianteDelProducto(producto, varianteId);
        }

        String extension = imagenUploadValidator.validarYObtenerExtension(file);
        String nombreArchivo = producto.getId() + "-" + UUID.randomUUID() + extension;

        try {
            Files.createDirectories(UPLOAD_DIR);
            Path destino = UPLOAD_DIR.resolve(nombreArchivo);
            file.transferTo(destino);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la imagen", ex);
        }

        ProductoFoto foto = new ProductoFoto();
        foto.setProducto(producto);
        foto.setVarianteId(varianteId);
        foto.setUrl("/uploads/productos/" + nombreArchivo);
        foto.setOrden(producto.getFotos().size());
        producto.getFotos().add(foto);

        return toDto(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoDto eliminarFoto(Long id, Long fotoId, UserPrincipal principal) {
        Producto producto = buscarPorEmpresa(id, principal);
        ProductoFoto foto = buscarFotoDelProducto(producto, fotoId);

        borrarArchivoSiExiste(foto.getUrl());
        producto.getFotos().remove(foto);
        reindexarOrden(producto);

        return toDto(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoDto reordenarFotos(Long id, List<Long> fotoIds, UserPrincipal principal) {
        Producto producto = buscarPorEmpresa(id, principal);
        Map<Long, ProductoFoto> porId = producto.getFotos().stream()
                .collect(Collectors.toMap(ProductoFoto::getId, foto -> foto));

        // Cualquier id que no pertenezca a ESTE producto (foto de otro
        // producto, o inexistente) rechaza toda la operación — mismo criterio
        // "todo o nada" que aplicarComponentes.
        for (Long fotoId : fotoIds) {
            if (!porId.containsKey(fotoId)) {
                throw new ProductoFotoNoEncontradaException();
            }
        }

        for (int orden = 0; orden < fotoIds.size(); orden++) {
            porId.get(fotoIds.get(orden)).setOrden(orden);
        }

        return toDto(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoDto asignarColorFoto(Long id, Long fotoId, Long varianteId, UserPrincipal principal) {
        Producto producto = buscarPorEmpresa(id, principal);
        ProductoFoto foto = buscarFotoDelProducto(producto, fotoId);
        if (varianteId != null) {
            buscarVarianteDelProducto(producto, varianteId);
        }
        foto.setVarianteId(varianteId);
        return toDto(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoDto ajustarFoto(Long id, Long fotoId, boolean agrandada, UserPrincipal principal) {
        Producto producto = buscarPorEmpresa(id, principal);
        ProductoFoto foto = buscarFotoDelProducto(producto, fotoId);
        foto.setAgrandada(agrandada);
        return toDto(productoRepository.save(producto));
    }

    private ProductoFoto buscarFotoDelProducto(Producto producto, Long fotoId) {
        return producto.getFotos().stream()
                .filter(foto -> foto.getId().equals(fotoId))
                .findFirst()
                .orElseThrow(ProductoFotoNoEncontradaException::new);
    }

    private ProductoVariante buscarVarianteDelProducto(Producto producto, Long varianteId) {
        return producto.getVariantes().stream()
                .filter(variante -> variante.getId().equals(varianteId))
                .findFirst()
                .orElseThrow(ProductoNoEncontradoException::new);
    }

    // Tras un borrado, compacta el orden de las fotos restantes a 0..n-1 —
    // mismo criterio que reordenarFotos, para que el próximo agregarFoto
    // (que apila al final con producto.getFotos().size()) no deje huecos.
    private void reindexarOrden(Producto producto) {
        List<ProductoFoto> ordenadas = producto.getFotos().stream()
                .sorted(Comparator.comparingInt(ProductoFoto::getOrden))
                .toList();
        for (int orden = 0; orden < ordenadas.size(); orden++) {
            ordenadas.get(orden).setOrden(orden);
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
    public FotoUploadDto subirFotoVariante(MultipartFile file, UserPrincipal principal) {
        // No persiste nada: solo valida acceso y escribe el archivo a disco.
        // La fotoUrl resultante la asocia el cliente a una fila de variante
        // recién cuando manda el POST/PUT /api/productos con ese valor —
        // mismo criterio que VentaServiceImpl.subirFoto.
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

        return new FotoUploadDto("/uploads/productos/" + nombreArchivo);
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
        return new ResenaDto(resena.getId(), resena.getClienteNombre(), resena.getComentario(),
                resena.getPuntuacion(), resena.getFecha(), resena.getImagenUrl(), resena.getVentaId() != null);
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
                                      List<ProductoInsumoRequest> insumosRequest,
                                      List<ProductoVarianteRequest> variantesRequest,
                                      List<ProductoComponenteRequest> componentesRequest,
                                      List<ProductoGrabadoRequest> grabadosRequest,
                                      Integer stock, BigDecimal costoUnitario, Long empresaId,
                                      List<Long> atributoValorIds) {
        producto.setNombre(nombre);
        producto.setCategoria(buscarCategoriaPorEmpresa(categoriaId, empresaId));
        producto.setSubcategoria(subcategoriaId != null
                ? buscarSubcategoriaDeCategoria(subcategoriaId, categoriaId)
                : null);
        producto.setDescripcion(descripcion);
        producto.setPrecioVenta(precioVenta);
        producto.setPrecioPorMayor(precioPorMayor);
        producto.setCantidadMinimaMayorista(cantidadMinimaMayorista);
        // Stock solo importa de verdad para un producto SIN receta ni kit
        // (ver StockDisponibleCalculator.calcular) — con receta o kit queda
        // inerte, nadie lo lee como fuente de verdad. costoUnitario en
        // cambio SIEMPRE se suma al costo (ver costoEfectivo más abajo).
        producto.setStock(stock != null ? stock : 0);
        producto.setCostoUnitario(costoUnitario);

        // Reemplazo completo de la lista: más simple que un diff y suficiente
        // para el caso de uso (el frontend siempre manda la receta entera).
        // orphanRemoval=true en Producto.insumos borra las filas viejas al
        // hacer flush. Puede llegar vacía (o null): un producto puede no
        // tener receta en absoluto (producto simple, stock propio de arriba).
        producto.getInsumos().clear();
        for (ProductoInsumoRequest insumoRequest : insumosRequest != null ? insumosRequest : List.<ProductoInsumoRequest>of()) {
            Insumo insumoMaestro = buscarInsumoPorEmpresa(insumoRequest.insumoId(), empresaId);
            ProductoInsumo productoInsumo = new ProductoInsumo();
            productoInsumo.setProducto(producto);
            productoInsumo.setInsumo(insumoMaestro);
            productoInsumo.setCantidad(insumoRequest.cantidad());
            producto.getInsumos().add(productoInsumo);
        }

        aplicarVariantes(producto, variantesRequest);
        aplicarComponentes(producto, componentesRequest, empresaId);
        aplicarGrabados(producto, grabadosRequest);
        aplicarAtributoValores(producto, atributoValorIds, categoriaId);
    }

    // Reemplazo completo (mismo criterio que insumos/grabados): cada id tiene
    // que ser un AtributoFiltroValor de un AtributoFiltro de LA categoría
    // elegida — nunca se confía en un id que venga del cliente sin validar
    // esa pertenencia (mismo criterio que buscarSubcategoriaDeCategoria).
    private void aplicarAtributoValores(Producto producto, List<Long> atributoValorIds, Long categoriaId) {
        producto.getAtributoValores().clear();
        if (atributoValorIds == null || atributoValorIds.isEmpty()) {
            return;
        }
        List<Long> atributoIdsDeLaCategoria = atributoFiltroRepository.findByCategoriaIdOrderByOrdenAsc(categoriaId).stream()
                .map(AtributoFiltro::getId)
                .toList();
        for (Long valorId : atributoValorIds) {
            AtributoFiltroValor valor = atributoFiltroValorRepository.findById(valorId)
                    .orElseThrow(AtributoFiltroValorNoEncontradoException::new);
            if (!atributoIdsDeLaCategoria.contains(valor.getAtributoFiltroId())) {
                throw new AtributoFiltroValorNoEncontradoException();
            }
            producto.getAtributoValores().add(valor);
        }
    }

    // Lugares grabables: reemplazo completo, mismo patrón que insumos/
    // componentes — nada externo referencia una fila de ProductoGrabado por
    // id de forma persistente (el pedido público solo la lee al momento de
    // armar el precio/texto de la venta, no guarda el id), así que no hace
    // falta el merge-por-id que sí necesita ProductoVariante.
    private void aplicarGrabados(Producto producto, List<ProductoGrabadoRequest> grabadosRequest) {
        producto.getGrabados().clear();
        for (ProductoGrabadoRequest grabadoRequest : grabadosRequest != null ? grabadosRequest : List.<ProductoGrabadoRequest>of()) {
            ProductoGrabado grabado = new ProductoGrabado();
            grabado.setProducto(producto);
            grabado.setLugar(grabadoRequest.lugar());
            grabado.setPrecio(grabadoRequest.precio());
            producto.getGrabados().add(grabado);
        }
    }

    // Kit (Composite): mismo patrón de reemplazo completo que insumos (nada
    // externo referencia una fila de ProductoComponente, a diferencia de
    // variantes). Dos reglas de negocio validadas acá, no en el DTO, porque
    // necesitan comparar contra el propio producto y contra el componente ya
    // resuelto de la base:
    //   - Auto-referencia: un producto no puede tenerse a sí mismo como
    //     componente (loop infinito en el cálculo de stock disponible).
    //   - Kit anidado: un componente no puede ser a su vez un kit, porque
    //     StockDisponibleCalculator.calcularKit no es recursivo — trataría
    //     al kit-componente como producto simple y el "sin stock automático"
    //     dejaría de funcionar para ese caso.
    private void aplicarComponentes(Producto producto, List<ProductoComponenteRequest> componentesRequest, Long empresaId) {
        List<ProductoComponenteRequest> requests = componentesRequest != null ? componentesRequest : List.of();

        producto.getComponentes().clear();
        for (ProductoComponenteRequest componenteRequest : requests) {
            if (componenteRequest.componenteProductoId().equals(producto.getId())) {
                throw new ProductoComponenteInvalidoException(
                        "Un producto no puede tenerse a sí mismo como componente del kit");
            }
            Producto componenteProducto = productoRepository
                    .findByIdAndEmpresaId(componenteRequest.componenteProductoId(), empresaId)
                    .orElseThrow(ProductoNoEncontradoException::new);
            if (!componenteProducto.getComponentes().isEmpty()) {
                throw new ProductoComponenteInvalidoException(
                        "\"" + componenteProducto.getNombre() + "\" ya es un kit y no puede usarse como componente de otro kit");
            }
            ProductoComponente productoComponente = new ProductoComponente();
            productoComponente.setProducto(producto);
            productoComponente.setComponenteProducto(componenteProducto);
            productoComponente.setCantidad(componenteRequest.cantidad());
            producto.getComponentes().add(productoComponente);
        }
    }

    // A diferencia de insumos (reemplazo completo arriba), acá se hace un
    // MERGE por id: una ProductoVariante no es una línea de receta, ES la
    // fuente de stock que VentaServiceImpl descuenta directo vía
    // VentaItem.varianteId. Si se recreara cada fila en cada edición (clear +
    // agregar todas nuevas), una venta ya cargada quedaría apuntando a una
    // fila borrada. Las filas con id existente se actualizan in-place, las
    // sin id (nuevas) se crean, y las que ya no vienen en la request se dan
    // de baja (orphanRemoval=true en Producto.variantes).
    private void aplicarVariantes(Producto producto, List<ProductoVarianteRequest> variantesRequest) {
        List<ProductoVarianteRequest> requests = variantesRequest != null ? variantesRequest : List.of();
        Map<Long, ProductoVariante> existentesPorId = producto.getVariantes().stream()
                .collect(Collectors.toMap(ProductoVariante::getId, v -> v));

        List<ProductoVariante> resultado = new ArrayList<>();
        for (ProductoVarianteRequest varianteRequest : requests) {
            ProductoVariante variante = varianteRequest.id() != null ? existentesPorId.get(varianteRequest.id()) : null;
            if (variante == null) {
                variante = new ProductoVariante();
                variante.setProducto(producto);
            }
            variante.setColor(varianteRequest.color());
            variante.setStock(varianteRequest.stock());
            variante.setColorHex(varianteRequest.colorHex());
            resultado.add(variante);
        }

        producto.getVariantes().clear();
        producto.getVariantes().addAll(resultado);
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
        List<ProductoFoto> fotosOrdenadas = producto.getFotos().stream()
                .sorted(Comparator.comparingInt(ProductoFoto::getOrden))
                .toList();
        List<ProductoFotoDto> fotos = fotosOrdenadas.stream()
                .map(foto -> new ProductoFotoDto(foto.getId(), foto.getUrl(), foto.getVarianteId(), foto.getOrden(), foto.isAgrandada()))
                .toList();
        List<ProductoInsumoDto> insumos = producto.getInsumos().stream()
                .map(this::toProductoInsumoDto)
                .toList();
        List<ProductoVarianteDto> variantes = producto.getVariantes().stream()
                .map(variante -> toProductoVarianteDto(variante, fotosOrdenadas))
                .toList();
        List<ProductoComponenteDto> componentes = producto.getComponentes().stream()
                .map(this::toProductoComponenteDto)
                .toList();
        List<ProductoGrabadoDto> grabados = producto.getGrabados().stream()
                .map(this::toProductoGrabadoDto)
                .toList();
        List<AtributoFiltroValorDto> atributoValores = producto.getAtributoValores().stream()
                .map(valor -> new AtributoFiltroValorDto(valor.getId(), valor.getValor()))
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
                // Derivado (ya no una columna): ver ProductoFotoResolver.
                productoFotoResolver.resolverMiniatura(fotosOrdenadas),
                fotos,
                // Calculado vía StockDisponibleCalculator (compartido con
                // PublicTiendaServiceImpl): así todo el sistema ve siempre el
                // mismo número sin saber si es un valor cargado a mano o
                // calculado a partir de insumos. Ver StockDisponibleCalculator
                // para el criterio completo.
                stockDisponibleCalculator.calcular(producto),
                costoEfectivo(producto, insumos),
                producto.getCostoUnitario(),
                insumos,
                variantes,
                componentes,
                grabados,
                atributoValores
        );
    }

    private ProductoVarianteDto toProductoVarianteDto(ProductoVariante variante, List<ProductoFoto> fotosOrdenadas) {
        String fotoUrl = variante.getId() != null
                ? productoFotoResolver.fotosDeVariante(fotosOrdenadas, variante.getId()).stream()
                        .findFirst()
                        .map(ProductoFoto::getUrl)
                        .orElse(null)
                : null;
        return new ProductoVarianteDto(
                variante.getId(), variante.getColor(), variante.getStock(),
                fotoUrl, variante.getColorHex());
    }

    private ProductoGrabadoDto toProductoGrabadoDto(ProductoGrabado grabado) {
        return new ProductoGrabadoDto(grabado.getId(), grabado.getLugar(), grabado.getPrecio());
    }

    private ProductoComponenteDto toProductoComponenteDto(ProductoComponente productoComponente) {
        Producto componenteProducto = productoComponente.getComponenteProducto();
        return new ProductoComponenteDto(
                productoComponente.getId(),
                componenteProducto.getId(),
                componenteProducto.getNombre(),
                productoComponente.getCantidad()
        );
    }

    // Costo propio del producto: siempre la suma del subtotal de cada línea
    // de SU receta directa (embalaje incluido si es un kit). No incluye el
    // costo de los componentes de un kit — ese cálculo agregado queda para
    // cuando el frontend lo necesite mostrar, hoy no se persiste ni se pide.
    private BigDecimal calcularCostoUnitario(List<ProductoInsumoDto> insumos) {
        return insumos.stream()
                .map(ProductoInsumoDto::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // El costo unitario propio (lo que le costó al usuario producir/comprar
    // ESE producto) y el costo de la receta de insumos (extras como bolsa o
    // sticker) son conceptos independientes: uno no reemplaza al otro. Antes
    // el costo de la receta pisaba por completo el costoUnitario apenas
    // había una línea de insumos — este suma siempre ambos.
    private BigDecimal costoEfectivo(Producto producto, List<ProductoInsumoDto> insumos) {
        BigDecimal costoPropio = producto.getCostoUnitario() != null ? producto.getCostoUnitario() : BigDecimal.ZERO;
        return costoPropio.add(calcularCostoUnitario(insumos));
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
