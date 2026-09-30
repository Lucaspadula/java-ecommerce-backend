package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.AtributoFiltroDto;
import com.sistventas.backend.dto.AtributoFiltroValorDto;
import com.sistventas.backend.dto.CategoriaDto;
import com.sistventas.backend.dto.CrearAtributoFiltroRequest;
import com.sistventas.backend.dto.CrearAtributoFiltroValorRequest;
import com.sistventas.backend.dto.CrearCategoriaRequest;
import com.sistventas.backend.dto.CrearSubcategoriaRequest;
import com.sistventas.backend.dto.SubcategoriaDto;
import com.sistventas.backend.entity.AtributoFiltro;
import com.sistventas.backend.entity.AtributoFiltroValor;
import com.sistventas.backend.entity.Categoria;
import com.sistventas.backend.entity.Subcategoria;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.exception.AtributoFiltroNoEncontradoException;
import com.sistventas.backend.exception.AtributoFiltroValorNoEncontradoException;
import com.sistventas.backend.exception.CategoriaNoEncontradaException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.exception.SubcategoriaNoEncontradaException;
import com.sistventas.backend.repository.AtributoFiltroRepository;
import com.sistventas.backend.repository.AtributoFiltroValorRepository;
import com.sistventas.backend.repository.CategoriaRepository;
import com.sistventas.backend.repository.SubcategoriaRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.CategoriaService;
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
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaRepository subcategoriaRepository;
    private final AtributoFiltroRepository atributoFiltroRepository;
    private final AtributoFiltroValorRepository atributoFiltroValorRepository;
    private final ImagenUploadValidator imagenUploadValidator;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository,
                                 SubcategoriaRepository subcategoriaRepository,
                                 AtributoFiltroRepository atributoFiltroRepository,
                                 AtributoFiltroValorRepository atributoFiltroValorRepository,
                                 ImagenUploadValidator imagenUploadValidator) {
        this.categoriaRepository = categoriaRepository;
        this.subcategoriaRepository = subcategoriaRepository;
        this.atributoFiltroRepository = atributoFiltroRepository;
        this.atributoFiltroValorRepository = atributoFiltroValorRepository;
        this.imagenUploadValidator = imagenUploadValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaDto> listar(UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        return categoriaRepository.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public CategoriaDto crear(CrearCategoriaRequest request, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        String nombre = request.nombre().trim();

        // Find-or-create (ver CrearCategoriaRequest): evita duplicados por
        // mayúsculas/espacios.
        Categoria categoria = categoriaRepository.findByEmpresaIdAndNombreIgnoreCase(empresaId, nombre)
                .orElseGet(() -> {
                    Categoria nueva = new Categoria();
                    nueva.setEmpresaId(empresaId);
                    nueva.setNombre(nombre);
                    return categoriaRepository.save(nueva);
                });
        return toDto(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubcategoriaDto> listarSubcategorias(Long categoriaId, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Categoria categoria = buscarCategoriaPorEmpresa(categoriaId, empresaId);
        return subcategoriaRepository.findByCategoriaIdOrderByNombreAsc(categoria.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public SubcategoriaDto crearSubcategoria(Long categoriaId, CrearSubcategoriaRequest request, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Categoria categoria = buscarCategoriaPorEmpresa(categoriaId, empresaId);
        String nombre = request.nombre().trim();

        Subcategoria subcategoria = subcategoriaRepository.findByCategoriaIdAndNombreIgnoreCase(categoria.getId(), nombre)
                .orElseGet(() -> {
                    Subcategoria nueva = new Subcategoria();
                    nueva.setCategoriaId(categoria.getId());
                    nueva.setNombre(nombre);
                    return subcategoriaRepository.save(nueva);
                });
        return toDto(subcategoria);
    }

    // Misma carpeta que TiendaCategoriaServiceImpl (imagen de categoría):
    // ambas son imágenes del mismo mega-menú/carrusel de categorías, no hace
    // falta separarlas en subcarpetas distintas. Mismo prefijo de nombre de
    // archivo con UUID para evitar colisiones entre categorías y
    // subcategorías. Validación con el ImagenUploadValidator compartido
    // (JPG/PNG/WEBP/GIF, 5MB máx) — ya no exige PNG/WEBP-con-transparencia:
    // eso solo tenía sentido para la imagen de categoría flotando sobre el
    // color del carrusel "Explorá por categoría", acá (y en el mega-menú)
    // la imagen va en un box propio, cualquier foto normal sirve.
    private static final Path UPLOAD_DIR = Paths.get("uploads", "categorias");

    @Override
    @Transactional
    public SubcategoriaDto actualizarImagenSubcategoria(Long categoriaId, Long subcategoriaId, MultipartFile file, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        Categoria categoria = buscarCategoriaPorEmpresa(categoriaId, empresaId);
        Subcategoria subcategoria = buscarSubcategoriaPorCategoria(subcategoriaId, categoria.getId());
        String extension = imagenUploadValidator.validarYObtenerExtension(file);

        String nombreArchivo = "subcategoria-" + empresaId + "-" + UUID.randomUUID() + extension;
        try {
            Files.createDirectories(UPLOAD_DIR);
            Path destino = UPLOAD_DIR.resolve(nombreArchivo);
            file.transferTo(destino);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la imagen de la subcategoría", ex);
        }

        // No se borra el archivo previo al reemplazar (mismo criterio que
        // TiendaCategoriaServiceImpl.actualizarImagen) — sí se borra en
        // eliminarImagenSubcategoria, donde se está pidiendo sacarla.
        subcategoria.setImagenUrl("/uploads/categorias/" + nombreArchivo);
        return toDto(subcategoriaRepository.save(subcategoria));
    }

    @Override
    @Transactional
    public SubcategoriaDto eliminarImagenSubcategoria(Long categoriaId, Long subcategoriaId, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        Categoria categoria = buscarCategoriaPorEmpresa(categoriaId, empresaId);
        Subcategoria subcategoria = buscarSubcategoriaPorCategoria(subcategoriaId, categoria.getId());

        borrarArchivoSiExiste(subcategoria.getImagenUrl());
        subcategoria.setImagenUrl(null);
        return toDto(subcategoriaRepository.save(subcategoria));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AtributoFiltroDto> listarAtributosFiltro(Long categoriaId, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Categoria categoria = buscarCategoriaPorEmpresa(categoriaId, empresaId);
        return atributoFiltroRepository.findByCategoriaIdOrderByOrdenAsc(categoria.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AtributoFiltroDto crearAtributoFiltro(Long categoriaId, CrearAtributoFiltroRequest request, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Categoria categoria = buscarCategoriaPorEmpresa(categoriaId, empresaId);
        String nombre = request.nombre().trim();

        AtributoFiltro atributo = atributoFiltroRepository.findByCategoriaIdAndNombreIgnoreCase(categoria.getId(), nombre)
                .orElseGet(() -> {
                    AtributoFiltro nuevo = new AtributoFiltro();
                    nuevo.setEmpresaId(empresaId);
                    nuevo.setCategoriaId(categoria.getId());
                    nuevo.setNombre(nombre);
                    nuevo.setOrden(atributoFiltroRepository.findByCategoriaIdOrderByOrdenAsc(categoria.getId()).size());
                    return atributoFiltroRepository.save(nuevo);
                });
        return toDto(atributo);
    }

    @Override
    @Transactional
    public void eliminarAtributoFiltro(Long categoriaId, Long atributoId, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Categoria categoria = buscarCategoriaPorEmpresa(categoriaId, empresaId);
        AtributoFiltro atributo = buscarAtributoPorCategoria(atributoId, categoria.getId());
        // Los valores del atributo se borran antes (la FK no tiene cascade a
        // nivel de esquema, ver V53__atributo_filtro.sql) — el join con
        // Producto (producto_atributo_valor) se limpia solo al no quedar
        // ninguna fila de atributo_filtro_valor con ese id.
        atributoFiltroValorRepository.deleteAll(
                atributoFiltroValorRepository.findByAtributoFiltroIdOrderByOrdenAsc(atributo.getId()));
        atributoFiltroRepository.delete(atributo);
    }

    @Override
    @Transactional
    public AtributoFiltroDto crearValorAtributoFiltro(
            Long categoriaId, Long atributoId, CrearAtributoFiltroValorRequest request, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Categoria categoria = buscarCategoriaPorEmpresa(categoriaId, empresaId);
        AtributoFiltro atributo = buscarAtributoPorCategoria(atributoId, categoria.getId());

        AtributoFiltroValor valor = new AtributoFiltroValor();
        valor.setAtributoFiltroId(atributo.getId());
        valor.setValor(request.valor().trim());
        valor.setOrden(atributoFiltroValorRepository.findByAtributoFiltroIdOrderByOrdenAsc(atributo.getId()).size());
        atributoFiltroValorRepository.save(valor);

        return toDto(atributo);
    }

    @Override
    @Transactional
    public void eliminarValorAtributoFiltro(Long categoriaId, Long atributoId, Long valorId, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Categoria categoria = buscarCategoriaPorEmpresa(categoriaId, empresaId);
        AtributoFiltro atributo = buscarAtributoPorCategoria(atributoId, categoria.getId());
        AtributoFiltroValor valor = atributoFiltroValorRepository.findByIdAndAtributoFiltroId(valorId, atributo.getId())
                .orElseThrow(AtributoFiltroValorNoEncontradoException::new);
        atributoFiltroValorRepository.delete(valor);
    }

    private AtributoFiltro buscarAtributoPorCategoria(Long atributoId, Long categoriaId) {
        return atributoFiltroRepository.findByIdAndCategoriaId(atributoId, categoriaId)
                .orElseThrow(AtributoFiltroNoEncontradoException::new);
    }

    private AtributoFiltroDto toDto(AtributoFiltro atributo) {
        List<AtributoFiltroValorDto> valores = atributoFiltroValorRepository
                .findByAtributoFiltroIdOrderByOrdenAsc(atributo.getId()).stream()
                .map(v -> new AtributoFiltroValorDto(v.getId(), v.getValor()))
                .toList();
        return new AtributoFiltroDto(atributo.getId(), atributo.getCategoriaId(), atributo.getNombre(), valores);
    }

    private Subcategoria buscarSubcategoriaPorCategoria(Long subcategoriaId, Long categoriaId) {
        return subcategoriaRepository.findByIdAndCategoriaId(subcategoriaId, categoriaId)
                .orElseThrow(SubcategoriaNoEncontradaException::new);
    }

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

    private Categoria buscarCategoriaPorEmpresa(Long categoriaId, Long empresaId) {
        return categoriaRepository.findByIdAndEmpresaId(categoriaId, empresaId)
                .orElseThrow(CategoriaNoEncontradaException::new);
    }

    private Long empresaIdOrThrow(UserPrincipal principal) {
        if (principal == null || principal.empresaId() == null) {
            throw new SinEmpresaException();
        }
        return principal.empresaId();
    }

    // Mismo criterio que TiendaCategoriaServiceImpl.adminEmpresaIdOrThrow:
    // solo para las dos acciones de imagen, el resto de la interfaz sigue
    // abierta a cualquier MEMBER (ver comentario del header de
    // CategoriaService).
    private Long adminEmpresaIdOrThrow(UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        if (principal.rolEmpresa() != RolEmpresa.ADMIN) {
            throw new AccesoRestringidoAdminException();
        }
        return empresaId;
    }

    private CategoriaDto toDto(Categoria categoria) {
        return new CategoriaDto(categoria.getId(), categoria.getNombre());
    }

    private SubcategoriaDto toDto(Subcategoria subcategoria) {
        return new SubcategoriaDto(subcategoria.getId(), subcategoria.getNombre(), subcategoria.getImagenUrl());
    }
}
