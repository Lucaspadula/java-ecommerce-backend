package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarCatalogoConfigRequest;
import com.sistventas.backend.dto.ActualizarEstiloTextoCatalogoRequest;
import com.sistventas.backend.dto.ActualizarGeminiApiKeyRequest;
import com.sistventas.backend.dto.ActualizarPerfilRequest;
import com.sistventas.backend.dto.ActualizarAparienciaRequest;
import com.sistventas.backend.dto.ActualizarDatosTiendaRequest;
import com.sistventas.backend.dto.ActualizarPromocionesRequest;
import com.sistventas.backend.dto.BannerImagenTiendaDto;
import com.sistventas.backend.dto.CambiarPasswordRequest;
import com.sistventas.backend.dto.MensajeResponse;
import com.sistventas.backend.dto.PerfilDto;
import com.sistventas.backend.entity.Empresa;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.TiendaBannerImagen;
import com.sistventas.backend.entity.Usuario;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.exception.AccionNoPermitidaException;
import com.sistventas.backend.exception.BannerImagenNoEncontradaException;
import com.sistventas.backend.exception.PasswordActualIncorrectaException;
import com.sistventas.backend.exception.ProductoNoEncontradoException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.exception.SlugEnUsoException;
import com.sistventas.backend.exception.UsuarioNoEncontradoException;
import com.sistventas.backend.repository.EmpresaRepository;
import com.sistventas.backend.repository.ProductoRepository;
import com.sistventas.backend.repository.TiendaBannerImagenRepository;
import com.sistventas.backend.repository.UsuarioRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.PerfilService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class PerfilServiceImpl implements PerfilService {

    private static final Path UPLOAD_DIR = Paths.get("uploads", "empresas");

    // Máximo de imágenes del carrusel de banner (hero) por empresa (ver
    // agregarBannerImagen): suficiente variedad para la rotación automática
    // de la tienda pública sin dejar subir una galería sin límite.
    private static final int MAX_BANNER_IMAGENES = 6;

    private static final String TIPO_BANNER_HERO = "HERO";

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImagenUploadValidator imagenUploadValidator;
    private final TiendaBannerImagenRepository tiendaBannerImagenRepository;
    private final ProductoRepository productoRepository;

    public PerfilServiceImpl(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            PasswordEncoder passwordEncoder,
            ImagenUploadValidator imagenUploadValidator,
            TiendaBannerImagenRepository tiendaBannerImagenRepository,
            ProductoRepository productoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.imagenUploadValidator = imagenUploadValidator;
        this.tiendaBannerImagenRepository = tiendaBannerImagenRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilDto obtener(UserPrincipal principal) {
        return toDto(buscarUsuario(principal));
    }

    @Override
    @Transactional
    public PerfilDto actualizar(ActualizarPerfilRequest request, UserPrincipal principal) {
        Usuario usuario = buscarUsuario(principal);
        usuario.setNombre(request.nombre());
        return toDto(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public MensajeResponse cambiarPassword(CambiarPasswordRequest request, UserPrincipal principal) {
        Usuario usuario = buscarUsuario(principal);
        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
            throw new PasswordActualIncorrectaException();
        }
        usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        usuarioRepository.save(usuario);
        return new MensajeResponse("Contraseña actualizada correctamente");
    }

    @Override
    @Transactional
    public PerfilDto actualizarLogoEmpresa(MultipartFile file, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        String extension = imagenUploadValidator.validarYObtenerExtension(file);
        String nombreArchivo = empresaId + "-" + UUID.randomUUID() + extension;

        try {
            Files.createDirectories(UPLOAD_DIR);
            Path destino = UPLOAD_DIR.resolve(nombreArchivo);
            file.transferTo(destino);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar el logo", ex);
        }

        Empresa empresa = empresaRepository.getReferenceById(empresaId);
        empresa.setLogoUrl("/uploads/empresas/" + nombreArchivo);
        empresaRepository.save(empresa);

        return toDto(buscarUsuario(principal));
    }

    @Override
    @Transactional
    public PerfilDto actualizarCatalogoPortadaImagen(MultipartFile file, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        String extension = imagenUploadValidator.validarYObtenerExtension(file);
        String nombreArchivo = empresaId + "-" + UUID.randomUUID() + extension;

        try {
            Files.createDirectories(UPLOAD_DIR);
            Path destino = UPLOAD_DIR.resolve(nombreArchivo);
            file.transferTo(destino);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la imagen de portada", ex);
        }

        Empresa empresa = empresaRepository.getReferenceById(empresaId);
        borrarArchivoBannerSiExiste(empresa.getCatalogoPortadaImagenUrl());
        empresa.setCatalogoPortadaImagenUrl("/uploads/empresas/" + nombreArchivo);
        empresaRepository.save(empresa);

        return toDto(buscarUsuario(principal));
    }

    @Override
    @Transactional
    public PerfilDto quitarCatalogoPortadaImagen(UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        Empresa empresa = empresaRepository.getReferenceById(empresaId);
        borrarArchivoBannerSiExiste(empresa.getCatalogoPortadaImagenUrl());
        empresa.setCatalogoPortadaImagenUrl(null);
        empresaRepository.save(empresa);

        return toDto(buscarUsuario(principal));
    }

    @Override
    @Transactional
    public PerfilDto actualizarApariencia(ActualizarAparienciaRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        Empresa empresa = empresaRepository.getReferenceById(empresaId);
        empresa.setTiendaBannerTagline(vacioComoNull(request.tiendaBannerTagline()));
        empresa.setTiendaBannerTitulo(vacioComoNull(request.tiendaBannerTitulo()));
        empresa.setTiendaBannerDescripcion(vacioComoNull(request.tiendaBannerDescripcion()));
        String fuente = vacioComoNull(request.tiendaFuente());
        empresa.setTiendaFuente(fuente != null ? fuente : "clasica");
        String tema = vacioComoNull(request.tiendaTema());
        empresa.setTiendaTema(tema != null ? tema : "claro");
        empresaRepository.save(empresa);

        return toDto(buscarUsuario(principal));
    }

    @Override
    @Transactional
    public PerfilDto actualizarPromociones(ActualizarPromocionesRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        Empresa empresa = empresaRepository.getReferenceById(empresaId);
        empresa.setTiendaCuponCodigo(vacioComoNull(request.tiendaCuponCodigo()));
        empresa.setTiendaCuponPorcentaje(request.tiendaCuponPorcentaje());
        empresa.setTiendaOfertaActiva(request.tiendaOfertaActiva());
        empresa.setTiendaOfertaEtiqueta(vacioComoNull(request.tiendaOfertaEtiqueta()));
        empresa.setTiendaOfertaTexto(vacioComoNull(request.tiendaOfertaTexto()));
        empresa.setTiendaOfertaFechaFin(request.tiendaOfertaFechaFin());
        empresaRepository.save(empresa);

        return toDto(buscarUsuario(principal));
    }

    @Override
    @Transactional
    public PerfilDto actualizarDatosTienda(ActualizarDatosTiendaRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);

        // Si el slug pedido ya lo tiene OTRA empresa, es conflicto. Si es la
        // misma empresa reguardando su propio slug (sin cambios o cambiando
        // solo tiendaHabilitada), no debe auto-bloquearse.
        empresaRepository.findBySlug(request.slug())
                .filter(otra -> !otra.getId().equals(empresaId))
                .ifPresent(otra -> { throw new SlugEnUsoException(); });

        Empresa empresa = empresaRepository.getReferenceById(empresaId);
        empresa.setSlug(request.slug());
        empresa.setTiendaHabilitada(request.tiendaHabilitada());
        empresa.setTiendaContactoWhatsapp(vacioComoNull(request.tiendaContactoWhatsapp()));
        empresa.setTiendaContactoInstagram(vacioComoNull(request.tiendaContactoInstagram()));
        empresa.setTiendaContactoEmail(vacioComoNull(request.tiendaContactoEmail()));
        empresa.setTiendaRazonSocial(vacioComoNull(request.tiendaRazonSocial()));
        empresa.setTiendaCuit(vacioComoNull(request.tiendaCuit()));
        empresa.setTiendaDireccion(vacioComoNull(request.tiendaDireccion()));
        empresa.setTiendaSobreNosotros(vacioComoNull(request.tiendaSobreNosotros()));
        empresaRepository.save(empresa);

        return toDto(buscarUsuario(principal));
    }

    @Override
    @Transactional
    public PerfilDto actualizarCatalogoConfig(ActualizarCatalogoConfigRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        Empresa empresa = empresaRepository.getReferenceById(empresaId);
        empresa.setCatalogoMostrarLogo(request.mostrarLogo());
        empresa.setCatalogoTituloPersonalizado(vacioComoNull(request.tituloPersonalizado()));
        empresa.setCatalogoMostrarDescripcion(request.mostrarDescripcion());
        empresa.setCatalogoMostrarColores(request.mostrarColores());
        empresa.setCatalogoColorFondoProductos(vacioComoNull(request.colorFondoProductos()));
        empresaRepository.save(empresa);
        return toDto(buscarUsuario(principal));
    }

    @Override
    @Transactional
    public PerfilDto actualizarEstiloTextoCatalogo(ActualizarEstiloTextoCatalogoRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        Empresa empresa = empresaRepository.getReferenceById(empresaId);
        empresa.setCatalogoTextoFuente(vacioComoNull(request.fuente()));
        empresa.setCatalogoTextoTamanio(request.tamanio());
        empresa.setCatalogoTextoColor(vacioComoNull(request.color()));
        empresaRepository.save(empresa);
        return toDto(buscarUsuario(principal));
    }

    @Override
    @Transactional
    public PerfilDto actualizarGeminiApiKey(ActualizarGeminiApiKeyRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        Empresa empresa = empresaRepository.getReferenceById(empresaId);
        empresa.setGeminiApiKey(vacioComoNull(request.apiKey()));
        empresaRepository.save(empresa);
        return toDto(buscarUsuario(principal));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerImagenTiendaDto> listarBannerImagenes(UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        return tiendaBannerImagenRepository.findByEmpresaIdOrderByOrden(empresaId).stream()
                .map(this::toBannerImagenDto)
                .toList();
    }

    @Override
    @Transactional
    public BannerImagenTiendaDto agregarBannerImagen(MultipartFile file, Long productoId, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);

        // Los banners verticales se retiraron (ahora son bloques): solo queda HERO.
        long cantidadActual = tiendaBannerImagenRepository.countByEmpresaIdAndTipo(empresaId, TIPO_BANNER_HERO);
        if (cantidadActual >= MAX_BANNER_IMAGENES) {
            throw new AccionNoPermitidaException(
                    "Máximo " + MAX_BANNER_IMAGENES + " imágenes en el banner. Sacá alguna para agregar otra.");
        }

        // Nunca se confía en un productoId que venga del cliente sin
        // validar que el producto sea de esta misma empresa (mismo criterio
        // que buscarPorEmpresa en ProductoServiceImpl).
        Long productoIdValidado = productoId != null ? validarProductoDeLaEmpresa(productoId, empresaId) : null;

        String extension = imagenUploadValidator.validarYObtenerExtension(file);
        String nombreArchivo = "banner-" + empresaId + "-" + UUID.randomUUID() + extension;

        try {
            Files.createDirectories(UPLOAD_DIR);
            Path destino = UPLOAD_DIR.resolve(nombreArchivo);
            file.transferTo(destino);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la imagen del banner", ex);
        }

        TiendaBannerImagen imagen = new TiendaBannerImagen();
        imagen.setEmpresaId(empresaId);
        imagen.setImagenUrl("/uploads/empresas/" + nombreArchivo);
        imagen.setOrden((int) cantidadActual);
        imagen.setTipo(TIPO_BANNER_HERO);
        imagen.setProductoId(productoIdValidado);

        return toBannerImagenDto(tiendaBannerImagenRepository.save(imagen));
    }

    @Override
    @Transactional
    public void eliminarBannerImagen(Long imagenId, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        TiendaBannerImagen imagen = tiendaBannerImagenRepository.findByIdAndEmpresaId(imagenId, empresaId)
                .orElseThrow(BannerImagenNoEncontradaException::new);

        borrarArchivoBannerSiExiste(imagen.getImagenUrl());
        tiendaBannerImagenRepository.delete(imagen);
    }

    @Override
    @Transactional
    public BannerImagenTiendaDto actualizarProductoBannerImagen(Long imagenId, Long productoId, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        TiendaBannerImagen imagen = tiendaBannerImagenRepository.findByIdAndEmpresaId(imagenId, empresaId)
                .orElseThrow(BannerImagenNoEncontradaException::new);

        Long productoIdValidado = productoId != null ? validarProductoDeLaEmpresa(productoId, empresaId) : null;
        imagen.setProductoId(productoIdValidado);

        return toBannerImagenDto(tiendaBannerImagenRepository.save(imagen));
    }

    // Nunca se confía en un productoId que venga del cliente sin este
    // chequeo, mismo criterio que buscarPorEmpresa en ProductoServiceImpl.
    private Long validarProductoDeLaEmpresa(Long productoId, Long empresaId) {
        Producto producto = productoRepository.findByIdAndEmpresaId(productoId, empresaId)
                .orElseThrow(ProductoNoEncontradoException::new);
        return producto.getId();
    }

    // Borrado best-effort: si el archivo ya no está o falla el delete, no
    // rompe la operación principal (borrar la fila es lo que importa; un
    // archivo huérfano en disco no es un problema crítico). Mismo criterio
    // que TiendaCategoriaServiceImpl.borrarArchivoSiExiste.
    private void borrarArchivoBannerSiExiste(String imagenUrl) {
        if (imagenUrl == null) {
            return;
        }
        String prefijo = "/uploads/empresas/";
        if (!imagenUrl.startsWith(prefijo)) {
            return;
        }
        try {
            Files.deleteIfExists(UPLOAD_DIR.resolve(imagenUrl.substring(prefijo.length())));
        } catch (IOException ignored) {
            // Best-effort: no bloquea la eliminación de la fila.
        }
    }

    // Un input vacío/solo-espacios se guarda como null, no como string vacío
    // — así el frontend/backend pueden distinguir "sin personalizar" (usa el
    // texto genérico) de "personalizado con texto vacío" (no tiene sentido).
    private String vacioComoNull(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }

    // El usuario del principal siempre existe (viene de un JWT válido), pero
    // se busca la entidad completa porque UserPrincipal solo trae lo mínimo
    // necesario para el scoping multiempresa.
    private Usuario buscarUsuario(UserPrincipal principal) {
        return usuarioRepository.findById(principal.id())
                .orElseThrow(UsuarioNoEncontradoException::new);
    }

    // Análogo a adminEmpresaIdOrThrow en UsuarioEmpresaServiceImpl: el logo de
    // la empresa solo lo puede subir el ADMIN, nunca un MEMBER ni el Super
    // Admin (que no tiene empresa).
    private Long adminEmpresaIdOrThrow(UserPrincipal principal) {
        if (principal == null || principal.empresaId() == null) {
            throw new SinEmpresaException();
        }
        if (principal.rolEmpresa() != RolEmpresa.ADMIN) {
            throw new AccesoRestringidoAdminException();
        }
        return principal.empresaId();
    }

    private PerfilDto toDto(Usuario usuario) {
        Empresa empresa = usuario.getEmpresa();
        return new PerfilDto(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRolEmpresa(),
                empresa != null ? empresa.getNombre() : null,
                usuario.isEsSuperAdmin(),
                empresa != null ? empresa.getLogoUrl() : null,
                empresa != null ? empresa.getSlug() : null,
                empresa != null && empresa.isTiendaHabilitada(),
                empresa != null ? empresa.getTiendaBannerTitulo() : null,
                empresa != null ? empresa.getTiendaBannerDescripcion() : null,
                empresa != null ? empresa.getTiendaBannerTagline() : null,
                empresa != null ? empresa.getTiendaContactoWhatsapp() : null,
                empresa != null ? empresa.getTiendaContactoInstagram() : null,
                empresa != null ? empresa.getTiendaContactoEmail() : null,
                empresa != null ? empresa.getTiendaCuponCodigo() : null,
                empresa != null ? empresa.getTiendaCuponPorcentaje() : null,
                empresa != null ? empresa.getTiendaFuente() : null,
                empresa != null ? empresa.getTiendaTema() : null,
                empresa != null ? empresa.getTiendaRazonSocial() : null,
                empresa != null ? empresa.getTiendaCuit() : null,
                empresa != null ? empresa.getTiendaDireccion() : null,
                empresa != null ? empresa.getTiendaSobreNosotros() : null,
                empresa != null && empresa.isTiendaOfertaActiva(),
                empresa != null ? empresa.getTiendaOfertaEtiqueta() : null,
                empresa != null ? empresa.getTiendaOfertaTexto() : null,
                empresa != null ? empresa.getTiendaOfertaFechaFin() : null,
                empresa != null && empresa.getGeminiApiKey() != null && !empresa.getGeminiApiKey().isBlank(),
                empresa != null ? empresa.getCatalogoPortadaImagenUrl() : null,
                empresa == null || empresa.isCatalogoMostrarLogo(),
                empresa != null ? empresa.getCatalogoTituloPersonalizado() : null,
                empresa == null || empresa.isCatalogoMostrarDescripcion(),
                empresa == null || empresa.isCatalogoMostrarColores(),
                empresa != null ? empresa.getCatalogoColorFondoProductos() : null,
                empresa != null ? empresa.getCatalogoTextoFuente() : null,
                empresa != null ? empresa.getCatalogoTextoTamanio() : null,
                empresa != null ? empresa.getCatalogoTextoColor() : null
        );
    }

    private BannerImagenTiendaDto toBannerImagenDto(TiendaBannerImagen imagen) {
        return new BannerImagenTiendaDto(imagen.getId(), imagen.getImagenUrl(), imagen.getOrden(), imagen.getTipo(), imagen.getProductoId());
    }
}
