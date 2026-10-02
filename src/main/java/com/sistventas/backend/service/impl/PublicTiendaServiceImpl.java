package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarPerfilClienteRequest;
import com.sistventas.backend.dto.BannerImagenPublicaDto;
import com.sistventas.backend.dto.AtributoFiltroDto;
import com.sistventas.backend.dto.AtributoFiltroValorDto;
import com.sistventas.backend.dto.CategoriaTiendaDto;
import com.sistventas.backend.dto.ClienteLoginDto;
import com.sistventas.backend.dto.ClienteLoginGoogleRequest;
import com.sistventas.backend.dto.ClienteLoginRequest;
import com.sistventas.backend.dto.ClienteLoginResponse;
import com.sistventas.backend.dto.CrearResenaClienteRequest;
import com.sistventas.backend.dto.RegistrarClienteGoogleRequest;
import com.sistventas.backend.dto.RegistrarClienteRequest;
import com.sistventas.backend.dto.FotoUploadDto;
import com.sistventas.backend.dto.PreviewDescuentoComboDto;
import com.sistventas.backend.dto.ProductoGrabadoDto;
import com.sistventas.backend.dto.PublicComponenteDto;
import com.sistventas.backend.dto.PreviewDescuentoComboRequest;
import com.sistventas.backend.dto.ProductoFotoDto;
import com.sistventas.backend.dto.PublicCategoriaMenuDto;
import com.sistventas.backend.dto.PublicEmpresaDto;
import com.sistventas.backend.dto.PublicSubcategoriaMenuDto;
import com.sistventas.backend.dto.PublicPedidoEstadoDto;
import com.sistventas.backend.dto.PublicPedidoHistorialDto;
import com.sistventas.backend.dto.PublicPedidoItemRequest;
import com.sistventas.backend.dto.PublicPedidoRequest;
import com.sistventas.backend.dto.PublicPedidoResultadoDto;
import com.sistventas.backend.dto.PublicProductoDto;
import com.sistventas.backend.dto.PublicVarianteDto;
import com.sistventas.backend.dto.ResenaDestacadaDto;
import com.sistventas.backend.dto.ResenaDto;
import com.sistventas.backend.entity.AtributoFiltro;
import com.sistventas.backend.entity.Categoria;
import com.sistventas.backend.entity.Cliente;
import com.sistventas.backend.entity.Empresa;
import com.sistventas.backend.entity.EstadoVenta;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ProductoFoto;
import com.sistventas.backend.entity.ProductoGrabado;
import com.sistventas.backend.entity.ProductoVariante;
import com.sistventas.backend.entity.ReglaDescuentoCombo;
import com.sistventas.backend.entity.Resena;
import com.sistventas.backend.entity.Subcategoria;
import com.sistventas.backend.entity.TiendaBannerImagen;
import com.sistventas.backend.entity.TiendaCategoria;
import com.sistventas.backend.entity.Venta;
import com.sistventas.backend.entity.VentaItem;
import com.sistventas.backend.exception.AccionNoPermitidaException;
import com.sistventas.backend.exception.CategoriaNoEncontradaException;
import com.sistventas.backend.exception.ClienteYaRegistradoException;
import com.sistventas.backend.exception.CredencialesInvalidasException;
import com.sistventas.backend.exception.CuponInvalidoException;
import com.sistventas.backend.exception.ProductoNoEncontradoException;
import com.sistventas.backend.exception.StockInsuficienteException;
import com.sistventas.backend.exception.TiendaNoEncontradaException;
import com.sistventas.backend.exception.VentaNoEncontradaException;
import com.sistventas.backend.repository.AtributoFiltroRepository;
import com.sistventas.backend.repository.AtributoFiltroValorRepository;
import com.sistventas.backend.repository.CategoriaRepository;
import com.sistventas.backend.repository.SubcategoriaRepository;
import com.sistventas.backend.repository.ClienteRepository;
import com.sistventas.backend.repository.EmpresaRepository;
import com.sistventas.backend.repository.ProductoRepository;
import com.sistventas.backend.repository.ReglaDescuentoComboRepository;
import com.sistventas.backend.repository.ResenaRepository;
import com.sistventas.backend.repository.TiendaBannerImagenRepository;
import com.sistventas.backend.repository.TiendaCategoriaRepository;
import com.sistventas.backend.repository.VentaEstadoHistorialRepository;
import com.sistventas.backend.repository.VentaRepository;
import com.sistventas.backend.security.GoogleTokenVerifier;
import com.sistventas.backend.security.JwtService;
import com.sistventas.backend.service.PublicTiendaService;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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

    // Directorio propio, separado de uploads/productos: la imagen la sube un
    // visitante ANÓNIMO (sin JWT, sin empresaId de por medio) para adjuntarla
    // a un pedido que todavía no existe — mismo criterio de "carpeta por
    // ciclo de vida" que UPLOAD_DIR_RESENAS en ProductoServiceImpl.
    private static final Path UPLOAD_DIR_GRABADOS = Paths.get("uploads", "grabados");

    private final EmpresaRepository empresaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final VentaRepository ventaRepository;
    private final VentaEstadoHistorialRepository ventaEstadoHistorialRepository;
    private final TiendaCategoriaRepository tiendaCategoriaRepository;
    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaRepository subcategoriaRepository;
    private final AtributoFiltroRepository atributoFiltroRepository;
    private final AtributoFiltroValorRepository atributoFiltroValorRepository;
    private final TiendaBannerImagenRepository tiendaBannerImagenRepository;
    private final ResenaRepository resenaRepository;
    private final StockDisponibleCalculator stockDisponibleCalculator;
    private final ReglaDescuentoComboRepository reglaDescuentoComboRepository;
    private final CalculadorDescuentoComboService calculadorDescuentoComboService;
    private final ImagenUploadValidator imagenUploadValidator;
    private final ProductoFotoResolver productoFotoResolver;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final com.sistventas.backend.repository.TiendaBloqueRepository tiendaBloqueRepository;
    private final com.sistventas.backend.repository.TiendaBloqueCardRepository tiendaBloqueCardRepository;

    public PublicTiendaServiceImpl(EmpresaRepository empresaRepository,
                                    ProductoRepository productoRepository,
                                    ClienteRepository clienteRepository,
                                    VentaRepository ventaRepository,
                                    VentaEstadoHistorialRepository ventaEstadoHistorialRepository,
                                    TiendaCategoriaRepository tiendaCategoriaRepository,
                                    CategoriaRepository categoriaRepository,
                                    SubcategoriaRepository subcategoriaRepository,
                                    AtributoFiltroRepository atributoFiltroRepository,
                                    AtributoFiltroValorRepository atributoFiltroValorRepository,
                                    TiendaBannerImagenRepository tiendaBannerImagenRepository,
                                    ResenaRepository resenaRepository,
                                    StockDisponibleCalculator stockDisponibleCalculator,
                                    ReglaDescuentoComboRepository reglaDescuentoComboRepository,
                                    CalculadorDescuentoComboService calculadorDescuentoComboService,
                                    ImagenUploadValidator imagenUploadValidator,
                                    ProductoFotoResolver productoFotoResolver,
                                    JwtService jwtService,
                                    PasswordEncoder passwordEncoder,
                                    GoogleTokenVerifier googleTokenVerifier,
                                    com.sistventas.backend.repository.TiendaBloqueRepository tiendaBloqueRepository,
                                    com.sistventas.backend.repository.TiendaBloqueCardRepository tiendaBloqueCardRepository) {
        this.tiendaBloqueRepository = tiendaBloqueRepository;
        this.tiendaBloqueCardRepository = tiendaBloqueCardRepository;
        this.empresaRepository = empresaRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.ventaRepository = ventaRepository;
        this.ventaEstadoHistorialRepository = ventaEstadoHistorialRepository;
        this.tiendaCategoriaRepository = tiendaCategoriaRepository;
        this.categoriaRepository = categoriaRepository;
        this.subcategoriaRepository = subcategoriaRepository;
        this.atributoFiltroRepository = atributoFiltroRepository;
        this.atributoFiltroValorRepository = atributoFiltroValorRepository;
        this.tiendaBannerImagenRepository = tiendaBannerImagenRepository;
        this.resenaRepository = resenaRepository;
        this.stockDisponibleCalculator = stockDisponibleCalculator;
        this.reglaDescuentoComboRepository = reglaDescuentoComboRepository;
        this.calculadorDescuentoComboService = calculadorDescuentoComboService;
        this.imagenUploadValidator = imagenUploadValidator;
        this.productoFotoResolver = productoFotoResolver;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicEmpresaDto obtenerEmpresa(String slug) {
        Empresa empresa = resolverEmpresa(slug);
        List<BannerImagenPublicaDto> bannerImagenes =
                tiendaBannerImagenRepository.findByEmpresaIdAndTipoOrderByOrden(empresa.getId(), "HERO").stream()
                        .map(this::toBannerImagenPublicaDto)
                        .toList();
        // Una fecha límite vencida oculta la barra sola, sin que el dueño
        // tenga que desactivarla a mano (ver Empresa.tiendaOfertaFechaFin).
        boolean ofertaVigente = empresa.isTiendaOfertaActiva()
                && empresa.getTiendaOfertaFechaFin() != null
                && empresa.getTiendaOfertaFechaFin().isAfter(LocalDateTime.now());
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
                empresa.getTiendaTema(),
                empresa.getTiendaRazonSocial(),
                empresa.getTiendaCuit(),
                empresa.getTiendaDireccion(),
                empresa.getTiendaSobreNosotros(),
                ofertaVigente ? empresa.getTiendaOfertaEtiqueta() : null,
                ofertaVigente ? empresa.getTiendaOfertaTexto() : null,
                ofertaVigente ? empresa.getTiendaOfertaFechaFin() : null
        );
    }

    @Override
    public FotoUploadDto subirFotoGrabado(String slug, MultipartFile file) {
        // No persiste nada, mismo criterio que ProductoServiceImpl.
        // subirFotoVariante: solo valida y escribe a disco, el cliente asocia
        // la URL resultante a un item recién al mandar POST .../pedidos. Se
        // valida el slug igual (no cualquier string arbitrario) aunque no se
        // use el resultado, para no dejar esto como un drop-box anónimo de
        // archivos sin ninguna relación con una tienda real.
        resolverEmpresa(slug);
        String extension = imagenUploadValidator.validarYObtenerExtension(file);
        String nombreArchivo = UUID.randomUUID() + extension;

        try {
            Files.createDirectories(UPLOAD_DIR_GRABADOS);
            Path destino = UPLOAD_DIR_GRABADOS.resolve(nombreArchivo);
            file.transferTo(destino);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la imagen", ex);
        }

        return new FotoUploadDto("/uploads/grabados/" + nombreArchivo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicProductoDto> listarProductos(String slug) {
        Empresa empresa = resolverEmpresa(slug);

        // Todas las reseñas de la empresa en un solo query (no un N+1 por
        // producto), agrupadas por productoId. La lista de origen ya viene
        // ordenada por fecha desc, así que el primer elemento de cada grupo
        // es la reseña MÁS RECIENTE de ese producto (para el teaser
        // resenaDestacada) — y del grupo entero sale el promedio/cantidad
        // (solo contando las de compra verificada, ver toProductoDto).
        Map<Long, List<Resena>> resenasPorProducto = resenaRepository.findByEmpresaIdOrderByFechaDesc(empresa.getId())
                .stream()
                .collect(Collectors.groupingBy(Resena::getProductoId));

        return productoRepository.findByEmpresaIdAndActivoTrue(empresa.getId()).stream()
                // No se muestra lo que no hay stock disponible. Usa el mismo
                // cálculo que ProductoServiceImpl (StockDisponibleCalculator):
                // para productos con receta esto no es Producto.stock, es lo
                // que alcanza a armar según el stock de insumos.
                .filter(producto -> stockDisponibleCalculator.calcular(producto) > 0)
                .map(producto -> toProductoDto(producto, resenasPorProducto.get(producto.getId())))
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
    public List<PublicCategoriaMenuDto> listarCategoriasMenu(String slug) {
        Empresa empresa = resolverEmpresa(slug);
        Long empresaId = empresa.getId();

        // Árbol para el mega-menú del navbar: a diferencia de listarCategorias
        // (que solo trae categorías con color/imagen configurados, para el
        // carrusel "Explorá por categoría"), acá van TODAS las categorías con
        // productos activos, tengan o no imagen — el mega-menú es navegación,
        // no una vidriera decorativa, así que una categoría sin imagen igual
        // tiene que aparecer como link. Mismo criterio para subcategorías.
        List<Producto> productosActivos = productoRepository.findByEmpresaIdAndActivoTrue(empresaId);
        Set<Long> categoriaIdsReales = productosActivos.stream()
                .map(producto -> producto.getCategoria().getId())
                .collect(Collectors.toSet());
        Set<Long> subcategoriaIdsReales = productosActivos.stream()
                .map(Producto::getSubcategoria)
                .filter(Objects::nonNull)
                .map(Subcategoria::getId)
                .collect(Collectors.toSet());

        Map<Long, TiendaCategoria> imagenPorCategoriaId = tiendaCategoriaRepository.findByEmpresaId(empresaId).stream()
                .collect(Collectors.toMap(TiendaCategoria::getCategoriaId, Function.identity()));

        return categoriaRepository.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .filter(categoria -> categoriaIdsReales.contains(categoria.getId()))
                .map(categoria -> {
                    List<PublicSubcategoriaMenuDto> subcategorias =
                            subcategoriaRepository.findByCategoriaIdOrderByNombreAsc(categoria.getId()).stream()
                                    .filter(sub -> subcategoriaIdsReales.contains(sub.getId()))
                                    .map(sub -> new PublicSubcategoriaMenuDto(sub.getId(), sub.getNombre(), sub.getImagenUrl()))
                                    .toList();
                    TiendaCategoria tiendaCategoria = imagenPorCategoriaId.get(categoria.getId());
                    String imagenUrl = tiendaCategoria != null ? tiendaCategoria.getImagenUrl() : null;
                    return new PublicCategoriaMenuDto(categoria.getId(), categoria.getNombre(), imagenUrl, subcategorias);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AtributoFiltroDto> listarAtributosFiltro(String slug, Long categoriaId) {
        Empresa empresa = resolverEmpresa(slug);
        Categoria categoria = categoriaRepository.findByIdAndEmpresaId(categoriaId, empresa.getId())
                .orElseThrow(CategoriaNoEncontradaException::new);
        return atributoFiltroRepository.findByCategoriaIdOrderByOrdenAsc(categoria.getId()).stream()
                .map(this::toAtributoFiltroDto)
                .toList();
    }

    private AtributoFiltroDto toAtributoFiltroDto(AtributoFiltro atributo) {
        List<AtributoFiltroValorDto> valores = atributoFiltroValorRepository
                .findByAtributoFiltroIdOrderByOrdenAsc(atributo.getId()).stream()
                .map(v -> new AtributoFiltroValorDto(v.getId(), v.getValor()))
                .toList();
        return new AtributoFiltroDto(atributo.getId(), atributo.getCategoriaId(), atributo.getNombre(), valores);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResenaDto> listarResenas(String slug, Long productoId) {
        Empresa empresa = resolverEmpresa(slug);
        Producto producto = buscarProductoActivo(productoId, empresa.getId());
        return resenaRepository.findByProductoIdOrderByFechaDesc(producto.getId()).stream()
                .map(r -> new ResenaDto(r.getId(), r.getClienteNombre(), r.getComentario(), r.getPuntuacion(),
                        r.getFecha(), r.getImagenUrl(), r.getVentaId() != null))
                .toList();
    }

    @Override
    @Transactional
    public ResenaDto crearResenaCliente(String slug, Long productoId, CrearResenaClienteRequest request) {
        Empresa empresa = resolverEmpresa(slug);
        Producto producto = buscarProductoActivo(productoId, empresa.getId());

        // Mismo criterio que consultarPedido: "venta no encontrada" cubre
        // tanto la venta inexistente como el teléfono que no coincide, así
        // no sirve para adivinar pedidos ajenos probando números al azar.
        Venta venta = ventaRepository.findByIdAndEmpresaId(request.ventaId(), empresa.getId())
                .orElseThrow(VentaNoEncontradaException::new);
        Cliente cliente = clienteRepository.findByIdAndEmpresaId(venta.getClienteId(), empresa.getId())
                .orElseThrow(VentaNoEncontradaException::new);
        String telefonoRecibido = request.telefono().trim();
        String telefonoCliente = cliente.getTelefono() != null ? cliente.getTelefono().trim() : "";
        if (!telefonoCliente.equals(telefonoRecibido)) {
            throw new VentaNoEncontradaException();
        }

        if (venta.getEstado() != EstadoVenta.ENTREGADA) {
            throw new AccionNoPermitidaException("Todavía no podés reseñar este producto: el pedido no está entregado.");
        }
        boolean productoEnLaVenta = venta.getItems().stream()
                .anyMatch(item -> productoId.equals(item.getProductoId()));
        if (!productoEnLaVenta) {
            throw new AccionNoPermitidaException("Este producto no forma parte de ese pedido.");
        }
        if (resenaRepository.existsByVentaIdAndProductoId(venta.getId(), productoId)) {
            throw new AccionNoPermitidaException("Ya dejaste una reseña de este producto para esta compra.");
        }

        Resena resena = new Resena();
        resena.setProductoId(productoId);
        resena.setEmpresaId(empresa.getId());
        resena.setClienteNombre(cliente.getNombre());
        resena.setComentario(vacioComoNull(request.comentario()));
        resena.setPuntuacion(request.puntuacion());
        resena.setVentaId(venta.getId());
        resena.setFecha(LocalDateTime.now());
        Resena guardada = resenaRepository.save(resena);

        return new ResenaDto(guardada.getId(), guardada.getClienteNombre(), guardada.getComentario(),
                guardada.getPuntuacion(), guardada.getFecha(), guardada.getImagenUrl(), true);
    }

    // --- Cuenta de cliente (login en la tienda pública) ---
    // Fase 1: solo login, sin registro todavía (ver plan acordado) — si no
    // existe cuenta, el error es el mismo genérico de siempre
    // (CredencialesInvalidasException), no un 404 que revele si el email
    // existe o no.

    @Override
    @Transactional(readOnly = true)
    public ClienteLoginResponse loginCliente(String slug, ClienteLoginRequest request) {
        Empresa empresa = resolverEmpresa(slug);
        Cliente cliente = clienteRepository.findByEmpresaIdAndEmail(empresa.getId(), request.email())
                .orElseThrow(CredencialesInvalidasException::new);

        if (cliente.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), cliente.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }

        return construirClienteLoginResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteLoginResponse loginClienteGoogle(String slug, ClienteLoginGoogleRequest request) {
        Empresa empresa = resolverEmpresa(slug);
        GoogleTokenVerifier.GoogleTokenInfo tokenInfo = googleTokenVerifier.verificar(request.idToken());

        // Primero por googleSub ya vinculado; si es la primera vez que este
        // cliente usa Google, cae al fallback por email — cubre tanto al
        // que ya se había registrado con contraseña como al que solo existe
        // como Cliente de una venta de invitado con ese mismo email cargado
        // en el checkout (Google confirma que es dueño de ese email, así
        // que vincularlo acá es seguro, no una suposición).
        Cliente cliente = clienteRepository.findByEmpresaIdAndGoogleSub(empresa.getId(), tokenInfo.sub())
                .or(() -> clienteRepository.findByEmpresaIdAndEmail(empresa.getId(), tokenInfo.email()))
                .orElseThrow(CredencialesInvalidasException::new);

        if (cliente.getGoogleSub() == null) {
            cliente.setGoogleSub(tokenInfo.sub());
            clienteRepository.save(cliente);
        }

        return construirClienteLoginResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteLoginResponse registrarCliente(String slug, RegistrarClienteRequest request) {
        Empresa empresa = resolverEmpresa(slug);
        String email = request.email().trim();
        String telefono = request.telefono().trim();

        Optional<Cliente> porEmail = clienteRepository.findByEmpresaIdAndEmail(empresa.getId(), email);
        if (porEmail.isPresent() && tieneCredenciales(porEmail.get())) {
            throw new ClienteYaRegistradoException("Ese email ya está registrado. Iniciá sesión.");
        }

        // Solo EMAIL identifica la cuenta — el teléfono ya no se usa para
        // buscar/fusionar con otra fila. Antes matcheaba (primero, después
        // como respaldo) por teléfono para "adoptar" una fila de compra de
        // invitado, pero dos clientes DISTINTOS que compartían el mismo
        // teléfono (typo, número de prueba reciclado) terminaban fusionados
        // en la MISMA fila: el segundo registro pisaba nombre/email/password
        // del primero, que pasaba a loguear como si fuera el segundo
        // (hallazgo del dueño, 2026-09: "cada vez que un usuario se loguea,
        // ingresa como test"). El teléfono no tiene por qué ser una clave de
        // identidad — se guarda como dato de contacto nada más.
        Cliente cliente = porEmail.orElseGet(Cliente::new);
        if (cliente.getId() == null) {
            cliente.setEmpresaId(empresa.getId());
            cliente.setActivo(true);
            cliente.setFechaAlta(LocalDateTime.now());
        }
        cliente.setNombre(request.nombre());
        cliente.setEmail(email);
        cliente.setTelefono(telefono);
        cliente.setPasswordHash(passwordEncoder.encode(request.password()));
        Cliente guardado = clienteRepository.save(cliente);

        // Mail de bienvenida: pendiente (falta la infra de envío de mails —
        // ver plan acordado, requiere API key de Resend).
        return construirClienteLoginResponse(guardado);
    }

    @Override
    @Transactional
    public ClienteLoginResponse registrarClienteGoogle(String slug, RegistrarClienteGoogleRequest request) {
        Empresa empresa = resolverEmpresa(slug);
        GoogleTokenVerifier.GoogleTokenInfo tokenInfo = googleTokenVerifier.verificar(request.idToken());
        String telefono = request.telefono().trim();

        if (clienteRepository.findByEmpresaIdAndGoogleSub(empresa.getId(), tokenInfo.sub()).isPresent()) {
            throw new ClienteYaRegistradoException("Ya existe una cuenta con este Google. Iniciá sesión.");
        }
        Optional<Cliente> porEmail = clienteRepository.findByEmpresaIdAndEmail(empresa.getId(), tokenInfo.email());
        if (porEmail.isPresent() && tieneCredenciales(porEmail.get())) {
            throw new ClienteYaRegistradoException("Ese email ya está registrado. Iniciá sesión.");
        }

        // Mismo criterio que registrarCliente: solo email identifica la
        // cuenta, el teléfono no se usa para buscar/fusionar con otra fila.
        Cliente cliente = porEmail.orElseGet(Cliente::new);
        if (cliente.getId() == null) {
            cliente.setEmpresaId(empresa.getId());
            cliente.setActivo(true);
            cliente.setFechaAlta(LocalDateTime.now());
            cliente.setNombre(tokenInfo.nombre() != null ? tokenInfo.nombre() : tokenInfo.email());
        }
        cliente.setEmail(tokenInfo.email());
        cliente.setTelefono(telefono);
        cliente.setGoogleSub(tokenInfo.sub());
        Cliente guardado = clienteRepository.save(cliente);

        return construirClienteLoginResponse(guardado);
    }

    private boolean tieneCredenciales(Cliente cliente) {
        return cliente.getPasswordHash() != null || cliente.getGoogleSub() != null;
    }

    private ClienteLoginResponse construirClienteLoginResponse(Cliente cliente) {
        String token = jwtService.generateTokenCliente(cliente);
        ClienteLoginDto clienteDto =
                new ClienteLoginDto(cliente.getId(), cliente.getNombre(), cliente.getEmail(), cliente.getTelefono());
        return new ClienteLoginResponse(token, clienteDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.sistventas.backend.dto.PublicTiendaBloqueDto> listarBloques(String slug) {
        Empresa empresa = resolverEmpresa(slug);
        Long empresaId = empresa.getId();
        List<com.sistventas.backend.entity.TiendaBloque> bloques =
                tiendaBloqueRepository.findByEmpresaIdAndActivoTrueOrderBySlotAscOrdenAscIdAsc(empresaId);
        if (bloques.isEmpty()) {
            return List.of();
        }

        // Una query de bloques + una de cards; los destinos PRODUCTO/CATEGORIA
        // se resuelven en lote (una query por tipo), nunca por card.
        List<Long> bloqueIds = bloques.stream().map(com.sistventas.backend.entity.TiendaBloque::getId).toList();
        List<com.sistventas.backend.entity.TiendaBloqueCard> cards =
                tiendaBloqueCardRepository.findByBloqueIdInOrderByOrdenAscIdAsc(bloqueIds);

        java.util.Set<Long> productoIds = idsDeAccion(cards, "PRODUCTO");
        java.util.Set<Long> categoriaIds = idsDeAccion(cards, "CATEGORIA");
        // Solo cuentan los de ESTA empresa: una referencia ajena o borrada baja a NINGUNA.
        java.util.Set<Long> productosValidos = productoIds.isEmpty() ? java.util.Set.of()
                : productoRepository.findAllById(productoIds).stream()
                        .filter(p -> empresaId.equals(p.getEmpresaId()))
                        .map(Producto::getId)
                        .collect(java.util.stream.Collectors.toSet());
        java.util.Map<Long, String> nombreCategoria = categoriaIds.isEmpty() ? java.util.Map.of()
                : categoriaRepository.findAllById(categoriaIds).stream()
                        .filter(c -> empresaId.equals(c.getEmpresaId()))
                        .collect(java.util.stream.Collectors.toMap(Categoria::getId, Categoria::getNombre));

        java.util.Map<Long, List<com.sistventas.backend.dto.PublicTiendaBloqueCardDto>> cardsPorBloque = new java.util.HashMap<>();
        for (com.sistventas.backend.entity.TiendaBloqueCard card : cards) {
            String accion = card.getAccion();
            String destino = null;
            switch (accion) {
                case "URL" -> destino = card.getAccionValor();
                case "PRODUCTO" -> {
                    Long id = parsearIdONull(card.getAccionValor());
                    destino = id != null && productosValidos.contains(id) ? String.valueOf(id) : null;
                }
                case "CATEGORIA" -> {
                    Long id = parsearIdONull(card.getAccionValor());
                    destino = id != null ? nombreCategoria.get(id) : null;
                }
                default -> { }
            }
            if (("URL".equals(accion) || "PRODUCTO".equals(accion) || "CATEGORIA".equals(accion)) && destino == null) {
                accion = "NINGUNA";
            }
            cardsPorBloque.computeIfAbsent(card.getBloqueId(), k -> new java.util.ArrayList<>())
                    .add(new com.sistventas.backend.dto.PublicTiendaBloqueCardDto(card.getId(), card.getImagenUrl(),
                            card.getOrientacion(), card.getTitulo(), card.getTexto(), accion, destino));
        }

        return bloques.stream()
                .map(b -> new com.sistventas.backend.dto.PublicTiendaBloqueDto(b.getId(), b.getTitulo(), b.getSlot(),
                        b.getAncho(), cardsPorBloque.getOrDefault(b.getId(), List.of())))
                .toList();
    }

    private java.util.Set<Long> idsDeAccion(List<com.sistventas.backend.entity.TiendaBloqueCard> cards, String accion) {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (com.sistventas.backend.entity.TiendaBloqueCard c : cards) {
            if (accion.equals(c.getAccion())) {
                Long id = parsearIdONull(c.getAccionValor());
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private Long parsearIdONull(String valor) {
        try {
            return valor == null ? null : Long.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
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

            // Producto con variantes de color: el cliente tiene que haber
            // elegido una (varianteId obligatorio acá, no a nivel DTO, porque
            // depende de si ESTE producto tiene variantes cargadas). El
            // disponible/precio salen de la variante puntual, no del total
            // del producto — dos colores del mismo mate no comparten stock.
            ProductoVariante variante = null;
            int disponible;
            if (!producto.getVariantes().isEmpty()) {
                if (itemRequest.varianteId() == null) {
                    throw new AccionNoPermitidaException("Elegí un color para " + producto.getNombre());
                }
                variante = producto.getVariantes().stream()
                        .filter(v -> v.getId().equals(itemRequest.varianteId()))
                        .findFirst()
                        .orElseThrow(ProductoNoEncontradoException::new);
                disponible = stockDisponibleCalculator.calcularVariante(variante);
                if (disponible < itemRequest.cantidad()) {
                    throw new StockInsuficienteException(
                            producto.getNombre() + " (" + variante.getColor() + ")", disponible, itemRequest.cantidad());
                }
            } else {
                // Mismo criterio que el catálogo (listarProductos): para
                // productos con receta, el disponible sale del stock de
                // insumos, no de Producto.stock.
                disponible = stockDisponibleCalculator.calcular(producto);
                if (disponible < itemRequest.cantidad()) {
                    throw new StockInsuficienteException(producto.getNombre(), disponible, itemRequest.cantidad());
                }
            }

            // Nunca se acepta un precio que venga del pedido público: siempre
            // sale del producto persistido. Todas las variantes cobran el
            // mismo precioVenta del Producto (sin override por color, ver
            // ProductoVarianteRequest).
            BigDecimal precioUnitario = producto.getPrecioVenta();

            // Grabado personalizado: opcional. Cada id de grabadoLugarIds
            // tiene que pertenecer a ESTE producto — se resuelve acá contra
            // el producto persistido, nunca se confía en un precio/lugar que
            // mande el cliente. Sin selección = sin cargo extra, item.
            // personalizacion queda null (comportamiento de siempre para
            // productos sin esta opción). precioUnitario del item NO incluye
            // el grabado a propósito (sigue siendo el precio de lista del
            // producto, para que cálculos de margen/reportes no se
            // contaminen); el cargo del servicio va directo al subtotal.
            BigDecimal precioGrabadoUnitario = BigDecimal.ZERO;
            String personalizacion = null;
            List<Long> grabadoLugarIds = itemRequest.grabadoLugarIds();
            if (grabadoLugarIds != null && !grabadoLugarIds.isEmpty()) {
                List<ProductoGrabado> lugaresElegidos = grabadoLugarIds.stream()
                        .map(lugarId -> producto.getGrabados().stream()
                                .filter(g -> g.getId().equals(lugarId))
                                .findFirst()
                                .orElseThrow(ProductoNoEncontradoException::new))
                        .toList();
                precioGrabadoUnitario = lugaresElegidos.stream()
                        .map(ProductoGrabado::getPrecio)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                String lugares = lugaresElegidos.stream()
                        .map(ProductoGrabado::getLugar)
                        .collect(Collectors.joining(", "));
                String textoGrabado = vacioComoNull(itemRequest.grabadoTexto());
                personalizacion = "Grabado en: " + lugares + (textoGrabado != null ? " — Texto: " + textoGrabado : "");
            }

            BigDecimal subtotalItem = precioUnitario.add(precioGrabadoUnitario)
                    .multiply(BigDecimal.valueOf(itemRequest.cantidad()));

            VentaItem item = new VentaItem();
            item.setVenta(venta);
            item.setProductoId(producto.getId());
            item.setProductoNombre(producto.getNombre());
            item.setCantidad(itemRequest.cantidad());
            item.setPrecioUnitario(precioUnitario);
            item.setSubtotal(subtotalItem);
            item.setPersonalizacion(personalizacion);
            item.setGrabadoImagenUrl(vacioComoNull(itemRequest.grabadoImagenUrl()));
            if (variante != null) {
                item.setVarianteId(variante.getId());
                // Snapshot, no un join en vivo: si el color se borra del form
                // de Producto después, esta venta vieja sigue mostrándolo
                // igual en el texto de WhatsApp (ver construirTextoCompartir
                // en VentaServiceImpl).
                item.setVarianteColor(variante.getColor());
            }
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

        return mapearPedidoEstado(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicPedidoEstadoDto> listarPedidosCliente(String slug, Long clienteId) {
        Empresa empresa = resolverEmpresa(slug);
        return ventaRepository.findByEmpresaIdAndClienteIdOrderByFechaPedidoDesc(empresa.getId(), clienteId).stream()
                .map(this::mapearPedidoEstado)
                .toList();
    }

    @Override
    @Transactional
    public ClienteLoginDto actualizarPerfilCliente(String slug, Long clienteId, ActualizarPerfilClienteRequest request) {
        Empresa empresa = resolverEmpresa(slug);
        Cliente cliente = clienteRepository.findByIdAndEmpresaId(clienteId, empresa.getId())
                .orElseThrow(CredencialesInvalidasException::new);

        cliente.setNombre(request.nombre().trim());
        cliente.setTelefono(request.telefono().trim());
        Cliente guardado = clienteRepository.save(cliente);

        return new ClienteLoginDto(guardado.getId(), guardado.getNombre(), guardado.getEmail(), guardado.getTelefono());
    }

    // Compartido por consultarPedido (una venta puntual, sin sesión) y
    // listarPedidosCliente (todas las de la cuenta logueada) — mismo mapeo,
    // incluido el historial de estados.
    private PublicPedidoEstadoDto mapearPedidoEstado(Venta venta) {
        List<PublicPedidoHistorialDto> historial = ventaEstadoHistorialRepository
                .findByVentaIdOrderByFechaDescIdDesc(venta.getId()).stream()
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

    private PublicProductoDto toProductoDto(Producto producto, List<Resena> resenas) {
        List<Resena> resenasDelProducto = resenas != null ? resenas : List.of();
        Resena resenaMasReciente = resenasDelProducto.stream().findFirst().orElse(null);
        ResenaDestacadaDto resenaDestacada = resenaMasReciente != null
                ? new ResenaDestacadaDto(resenaMasReciente.getClienteNombre(), resenaMasReciente.getComentario())
                : null;
        // Promedio/cantidad SOLO de reseñas con compra verificada (ventaId no
        // null): las cargadas a mano por el admin tienen puntuacion=5 por
        // default de columna, no un rating real, así que mezclarlas
        // inflaría el promedio artificialmente.
        List<Resena> resenasVerificadas = resenasDelProducto.stream()
                .filter(r -> r.getVentaId() != null)
                .toList();
        Double promedioResenas = resenasVerificadas.isEmpty()
                ? null
                : resenasVerificadas.stream().mapToInt(Resena::getPuntuacion).average().orElse(0);
        Integer cantidadResenas = resenasVerificadas.size();
        List<ProductoFoto> fotosOrdenadas = producto.getFotos().stream()
                .sorted(java.util.Comparator.comparingInt(ProductoFoto::getOrden))
                .toList();
        List<ProductoFotoDto> fotos = fotosOrdenadas.stream()
                .map(foto -> new ProductoFotoDto(foto.getId(), foto.getUrl(), foto.getVarianteId(), foto.getOrden(), foto.isAgrandada()))
                .toList();
        List<PublicVarianteDto> variantes = producto.getVariantes().stream()
                .map(variante -> new PublicVarianteDto(
                        variante.getId(),
                        variante.getColor(),
                        stockDisponibleCalculator.calcularVariante(variante),
                        productoFotoResolver.fotosDeVariante(fotosOrdenadas, variante.getId()).stream()
                                .findFirst()
                                .map(ProductoFoto::getUrl)
                                .orElse(null),
                        variante.getColorHex()))
                .toList();
        List<PublicComponenteDto> componentes = producto.getComponentes().stream()
                .map(componente -> new PublicComponenteDto(
                        componente.getComponenteProducto().getNombre(),
                        componente.getCantidad()))
                .toList();
        List<ProductoGrabadoDto> grabados = producto.getGrabados().stream()
                .map(grabado -> new ProductoGrabadoDto(grabado.getId(), grabado.getLugar(), grabado.getPrecio()))
                .toList();
        List<AtributoFiltroValorDto> atributoValores = producto.getAtributoValores().stream()
                .map(valor -> new AtributoFiltroValorDto(valor.getId(), valor.getValor()))
                .toList();
        return new PublicProductoDto(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getCategoria().getNombre(),
                producto.getSubcategoria() != null ? producto.getSubcategoria().getNombre() : null,
                producto.getCategoria().getId(),
                producto.getSubcategoria() != null ? producto.getSubcategoria().getId() : null,
                producto.getPrecioVenta(),
                productoFotoResolver.resolverMiniatura(fotosOrdenadas),
                fotos,
                stockDisponibleCalculator.calcular(producto),
                resenaDestacada,
                promedioResenas,
                cantidadResenas,
                variantes,
                componentes,
                grabados,
                atributoValores
        );
    }
}
