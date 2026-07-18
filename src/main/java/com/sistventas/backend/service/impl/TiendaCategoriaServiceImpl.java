package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarColorCategoriaRequest;
import com.sistventas.backend.dto.CategoriaTiendaDto;
import com.sistventas.backend.entity.Categoria;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.TiendaCategoria;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.exception.ArchivoInvalidoException;
import com.sistventas.backend.exception.CategoriaNoEncontradaException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.CategoriaRepository;
import com.sistventas.backend.repository.TiendaCategoriaRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.TiendaCategoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Metadata visual de categorías de producto (color + imagen), para el
 * carrusel "Explorá por categoría" de la tienda pública. Separado de
 * PerfilService a propósito: PerfilServiceImpl gira en torno a
 * Usuario/Empresa y siempre devuelve PerfilDto, mientras que esta feature
 * tiene su propia tabla (tienda_categoria), su propio DTO (lista de
 * CategoriaTiendaDto) y necesita CategoriaRepository para resolver el
 * catálogo maestro de categorías — mezclar ambas hubiera inflado
 * PerfilServiceImpl con una responsabilidad distinta.
 */
@Service
public class TiendaCategoriaServiceImpl implements TiendaCategoriaService {

    private static final Path UPLOAD_DIR = Paths.get("uploads", "categorias");

    // Whitelist exacta por Content-Type: a diferencia del resto de los
    // uploads del proyecto (que solo chequean el prefijo "image/" y derivan
    // la extensión del nombre de archivo original sin sanitizar), acá se
    // valida contra un set cerrado y la extensión en disco sale del
    // Content-Type ya validado, nunca del nombre que manda el cliente. Solo
    // PNG/WEBP: son los únicos formatos de la whitelist con canal alfa
    // (transparencia), necesaria para que la imagen se recorte bien sobre el
    // fondo de color de la categoría.
    private static final Map<String, String> CONTENT_TYPE_A_EXTENSION = Map.of(
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final TiendaCategoriaRepository tiendaCategoriaRepository;
    private final CategoriaRepository categoriaRepository;

    public TiendaCategoriaServiceImpl(TiendaCategoriaRepository tiendaCategoriaRepository,
                                       CategoriaRepository categoriaRepository) {
        this.tiendaCategoriaRepository = tiendaCategoriaRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaTiendaDto> listar(UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);

        // DECISIÓN (refactor a tabla maestra): antes esto listaba solo las
        // categorías usadas por productos ACTIVOS. Ahora que Categoria es un
        // catálogo maestro real por empresa, se listan TODAS las categorías
        // de la empresa (incluida una recién creada sin productos todavía) —
        // más simple y más correcto: el admin puede personalizar color/imagen
        // de una categoría antes de cargarle productos.
        List<Categoria> categorias = categoriaRepository.findByEmpresaIdOrderByNombreAsc(empresaId);
        Map<Long, TiendaCategoria> configuradasPorCategoriaId = configuradasPorCategoriaId(empresaId);

        return categorias.stream()
                .map(categoria -> toDto(categoria, configuradasPorCategoriaId.get(categoria.getId())))
                .toList();
    }

    @Override
    @Transactional
    public CategoriaTiendaDto actualizarColor(Long categoriaId, ActualizarColorCategoriaRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        Categoria categoria = buscarCategoriaPorEmpresa(categoriaId, empresaId);
        TiendaCategoria tiendaCategoria = buscarOCrear(empresaId, categoria.getId());
        tiendaCategoria.setColor(request.color());
        return toDto(categoria, tiendaCategoriaRepository.save(tiendaCategoria));
    }

    @Override
    @Transactional
    public CategoriaTiendaDto actualizarImagen(Long categoriaId, MultipartFile file, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        Categoria categoria = buscarCategoriaPorEmpresa(categoriaId, empresaId);
        String extension = validarImagenYObtenerExtension(file);
        TiendaCategoria tiendaCategoria = buscarOCrear(empresaId, categoria.getId());

        String nombreArchivo = "categoria-" + empresaId + "-" + UUID.randomUUID() + extension;
        try {
            Files.createDirectories(UPLOAD_DIR);
            Path destino = UPLOAD_DIR.resolve(nombreArchivo);
            file.transferTo(destino);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la imagen de la categoría", ex);
        }

        // No se borra el archivo previo en disco al reemplazar: mismo
        // criterio que ProductoServiceImpl.actualizarFoto (que tampoco lo
        // hace), así que el comportamiento es consistente en todo el
        // proyecto. Sí se borra explícitamente en eliminarImagen, donde el
        // usuario está pidiendo sacar la imagen (no reemplazarla).
        tiendaCategoria.setImagenUrl("/uploads/categorias/" + nombreArchivo);
        return toDto(categoria, tiendaCategoriaRepository.save(tiendaCategoria));
    }

    @Override
    @Transactional
    public CategoriaTiendaDto eliminarImagen(Long categoriaId, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        Categoria categoria = buscarCategoriaPorEmpresa(categoriaId, empresaId);
        TiendaCategoria tiendaCategoria = buscarOCrear(empresaId, categoria.getId());

        borrarArchivoSiExiste(tiendaCategoria.getImagenUrl());
        tiendaCategoria.setImagenUrl(null);
        return toDto(categoria, tiendaCategoriaRepository.save(tiendaCategoria));
    }

    private Categoria buscarCategoriaPorEmpresa(Long categoriaId, Long empresaId) {
        return categoriaRepository.findByIdAndEmpresaId(categoriaId, empresaId)
                .orElseThrow(CategoriaNoEncontradaException::new);
    }

    private Map<Long, TiendaCategoria> configuradasPorCategoriaId(Long empresaId) {
        return tiendaCategoriaRepository.findByEmpresaId(empresaId).stream()
                .collect(Collectors.toMap(TiendaCategoria::getCategoriaId, Function.identity()));
    }

    private TiendaCategoria buscarOCrear(Long empresaId, Long categoriaId) {
        return tiendaCategoriaRepository.findByEmpresaIdAndCategoriaId(empresaId, categoriaId)
                .orElseGet(() -> {
                    TiendaCategoria nueva = new TiendaCategoria();
                    nueva.setEmpresaId(empresaId);
                    nueva.setCategoriaId(categoriaId);
                    return nueva;
                });
    }

    // Borrado best-effort: si el archivo ya no está o falla el delete, no
    // rompe la operación principal (actualizar la metadata en la base es lo
    // que importa; un archivo huérfano en disco no es un problema crítico).
    private void borrarArchivoSiExiste(String imagenUrl) {
        if (imagenUrl == null) {
            return;
        }
        String prefijo = "/uploads/categorias/";
        if (!imagenUrl.startsWith(prefijo)) {
            return;
        }
        try {
            Files.deleteIfExists(UPLOAD_DIR.resolve(imagenUrl.substring(prefijo.length())));
        } catch (IOException ignored) {
            // Best-effort: no bloquea la actualización de la metadata.
        }
    }

    private String validarImagenYObtenerExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ArchivoInvalidoException("El archivo es obligatorio");
        }
        String extension = CONTENT_TYPE_A_EXTENSION.get(file.getContentType());
        if (extension == null) {
            throw new ArchivoInvalidoException("La imagen debe ser PNG o WEBP (con transparencia)");
        }
        return extension;
    }

    // Único punto donde se resuelve empresaId del usuario logueado, exigiendo
    // además rol ADMIN: mismo criterio que adminEmpresaIdOrThrow en
    // PerfilServiceImpl (la personalización visual de la tienda es una
    // acción de administración, no la puede hacer un MEMBER).
    private Long adminEmpresaIdOrThrow(UserPrincipal principal) {
        if (principal == null || principal.empresaId() == null) {
            throw new SinEmpresaException();
        }
        if (principal.rolEmpresa() != RolEmpresa.ADMIN) {
            throw new AccesoRestringidoAdminException();
        }
        return principal.empresaId();
    }

    private CategoriaTiendaDto toDto(Categoria categoria, TiendaCategoria tiendaCategoria) {
        if (tiendaCategoria == null) {
            return new CategoriaTiendaDto(categoria.getId(), categoria.getNombre(), null, null);
        }
        return new CategoriaTiendaDto(categoria.getId(), categoria.getNombre(), tiendaCategoria.getColor(), tiendaCategoria.getImagenUrl());
    }
}
