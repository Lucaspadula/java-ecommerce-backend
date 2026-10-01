package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.BloqueImagenUploadDto;
import com.sistventas.backend.dto.GuardarTiendaBloqueCardRequest;
import com.sistventas.backend.dto.GuardarTiendaBloqueRequest;
import com.sistventas.backend.dto.ReordenarBloquesRequest;
import com.sistventas.backend.dto.ReordenarCardsRequest;
import com.sistventas.backend.dto.TiendaBloqueCardDto;
import com.sistventas.backend.dto.TiendaBloqueDto;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.TiendaBloque;
import com.sistventas.backend.entity.TiendaBloqueCard;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.exception.BloqueTiendaInvalidoException;
import com.sistventas.backend.exception.BloqueTiendaNoEncontradoException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.CategoriaRepository;
import com.sistventas.backend.repository.ProductoRepository;
import com.sistventas.backend.repository.TiendaBloqueCardRepository;
import com.sistventas.backend.repository.TiendaBloqueRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.TiendaBloqueService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Bloques configurables de la tienda pública (reemplazan a los banners
 * verticales y los tips fijos). Todas las validaciones de valores viven acá y
 * devuelven 400 (BloqueTiendaInvalidoException); operar sobre un recurso de
 * otra empresa devuelve 404 (BloqueTiendaNoEncontradoException).
 */
@Service
public class TiendaBloqueServiceImpl implements TiendaBloqueService {

    private static final Path UPLOAD_DIR = Paths.get("uploads", "bloques");

    private final TiendaBloqueRepository bloqueRepository;
    private final TiendaBloqueCardRepository cardRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ImagenUploadValidator imagenUploadValidator;

    public TiendaBloqueServiceImpl(TiendaBloqueRepository bloqueRepository,
                                    TiendaBloqueCardRepository cardRepository,
                                    ProductoRepository productoRepository,
                                    CategoriaRepository categoriaRepository,
                                    ImagenUploadValidator imagenUploadValidator) {
        this.bloqueRepository = bloqueRepository;
        this.cardRepository = cardRepository;
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.imagenUploadValidator = imagenUploadValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TiendaBloqueDto> listar(UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        return conCards(bloqueRepository.findByEmpresaIdOrderBySlotAscOrdenAscIdAsc(empresaId));
    }

    @Override
    @Transactional
    public TiendaBloqueDto crear(GuardarTiendaBloqueRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        validarSlotYAncho(request);
        if (bloqueRepository.countByEmpresaId(empresaId) >= TiendaBloqueCatalogo.MAX_BLOQUES_POR_EMPRESA) {
            throw new BloqueTiendaInvalidoException(
                    "Se alcanzó el máximo de " + TiendaBloqueCatalogo.MAX_BLOQUES_POR_EMPRESA + " bloques");
        }

        TiendaBloque bloque = new TiendaBloque();
        bloque.setEmpresaId(empresaId);
        bloque.setTitulo(limpiar(request.titulo()));
        bloque.setSlot(request.slot());
        bloque.setAncho(request.ancho());
        bloque.setActivo(request.activo() == null || request.activo());
        // Al final del slot: orden = cantidad actual de bloques en ese slot.
        bloque.setOrden((int) bloqueRepository.countByEmpresaIdAndSlot(empresaId, request.slot()));

        return toDto(bloqueRepository.save(bloque), List.of());
    }

    @Override
    @Transactional
    public TiendaBloqueDto actualizar(Long id, GuardarTiendaBloqueRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        TiendaBloque bloque = bloqueDeLaEmpresa(id, empresaId);
        validarSlotYAncho(request);

        if (!request.slot().equals(bloque.getSlot())) {
            // Cambio de slot: pasa al final del slot nuevo.
            bloque.setOrden((int) bloqueRepository.countByEmpresaIdAndSlot(empresaId, request.slot()));
            bloque.setSlot(request.slot());
        }
        bloque.setTitulo(limpiar(request.titulo()));
        bloque.setAncho(request.ancho());
        if (request.activo() != null) {
            bloque.setActivo(request.activo());
        }

        return toDtoConCards(bloqueRepository.save(bloque));
    }

    @Override
    @Transactional
    public void eliminar(Long id, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        TiendaBloque bloque = bloqueDeLaEmpresa(id, empresaId);
        // Primero las cards (explícito); el ON DELETE CASCADE queda de respaldo.
        cardRepository.deleteByBloqueId(bloque.getId());
        bloqueRepository.delete(bloque);
    }

    @Override
    @Transactional
    public TiendaBloqueDto actualizarActivo(Long id, boolean activo, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        TiendaBloque bloque = bloqueDeLaEmpresa(id, empresaId);
        bloque.setActivo(activo);
        return toDtoConCards(bloqueRepository.save(bloque));
    }

    @Override
    @Transactional
    public List<TiendaBloqueDto> reordenar(ReordenarBloquesRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        if (request == null || request.slot() == null || !TiendaBloqueCatalogo.SLOTS.contains(request.slot())) {
            throw new BloqueTiendaInvalidoException("Slot inválido");
        }
        List<Long> ids = request.ids() == null ? List.of() : request.ids();

        List<TiendaBloque> delSlot = bloqueRepository.findByEmpresaIdAndSlotOrderByOrdenAscIdAsc(empresaId, request.slot());
        Map<Long, TiendaBloque> porId = delSlot.stream().collect(Collectors.toMap(TiendaBloque::getId, Function.identity()));
        // Un id fuera del slot de ESTA empresa (otra empresa, otro slot, inexistente): no encontrado.
        if (ids.stream().anyMatch(i -> !porId.containsKey(i))) {
            throw new BloqueTiendaNoEncontradoException();
        }
        if (new HashSet<>(ids).size() != ids.size() || ids.size() != delSlot.size()) {
            throw new BloqueTiendaInvalidoException("La lista de orden debe contener todos los bloques del slot, sin repetir");
        }

        List<TiendaBloque> ordenados = new java.util.ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            TiendaBloque bloque = porId.get(ids.get(i));
            bloque.setOrden(i);
            ordenados.add(bloque);
        }
        bloqueRepository.saveAll(ordenados);
        return conCards(ordenados);
    }

    @Override
    @Transactional
    public TiendaBloqueCardDto crearCard(Long bloqueId, GuardarTiendaBloqueCardRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        TiendaBloque bloque = bloqueDeLaEmpresa(bloqueId, empresaId);
        long cantidad = cardRepository.countByBloqueId(bloque.getId());
        if (cantidad >= TiendaBloqueCatalogo.MAX_CARDS_POR_BLOQUE) {
            throw new BloqueTiendaInvalidoException(
                    "Se alcanzó el máximo de " + TiendaBloqueCatalogo.MAX_CARDS_POR_BLOQUE + " cards por bloque");
        }

        TiendaBloqueCard card = new TiendaBloqueCard();
        card.setBloqueId(bloque.getId());
        card.setOrden((int) cantidad);
        aplicar(card, request, empresaId);
        return toCardDto(cardRepository.save(card));
    }

    @Override
    @Transactional
    public TiendaBloqueCardDto actualizarCard(Long bloqueId, Long cardId, GuardarTiendaBloqueCardRequest request,
                                              UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        TiendaBloque bloque = bloqueDeLaEmpresa(bloqueId, empresaId);
        TiendaBloqueCard card = cardRepository.findByIdAndBloqueId(cardId, bloque.getId())
                .orElseThrow(BloqueTiendaNoEncontradoException::new);
        aplicar(card, request, empresaId);
        return toCardDto(cardRepository.save(card));
    }

    @Override
    @Transactional
    public void eliminarCard(Long bloqueId, Long cardId, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        TiendaBloque bloque = bloqueDeLaEmpresa(bloqueId, empresaId);
        TiendaBloqueCard card = cardRepository.findByIdAndBloqueId(cardId, bloque.getId())
                .orElseThrow(BloqueTiendaNoEncontradoException::new);
        cardRepository.delete(card);
    }

    @Override
    @Transactional
    public List<TiendaBloqueCardDto> reordenarCards(Long bloqueId, ReordenarCardsRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        TiendaBloque bloque = bloqueDeLaEmpresa(bloqueId, empresaId);
        List<Long> ids = request == null || request.ids() == null ? List.of() : request.ids();

        List<TiendaBloqueCard> actuales = cardRepository.findByBloqueIdOrderByOrdenAscIdAsc(bloque.getId());
        Map<Long, TiendaBloqueCard> porId = actuales.stream().collect(Collectors.toMap(TiendaBloqueCard::getId, Function.identity()));
        if (ids.stream().anyMatch(i -> !porId.containsKey(i))) {
            throw new BloqueTiendaNoEncontradoException();
        }
        if (new HashSet<>(ids).size() != ids.size() || ids.size() != actuales.size()) {
            throw new BloqueTiendaInvalidoException("La lista de orden debe contener todas las cards del bloque, sin repetir");
        }

        List<TiendaBloqueCard> ordenadas = new java.util.ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            TiendaBloqueCard card = porId.get(ids.get(i));
            card.setOrden(i);
            ordenadas.add(card);
        }
        cardRepository.saveAll(ordenadas);
        return ordenadas.stream().map(this::toCardDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BloqueImagenUploadDto subirImagen(MultipartFile file, UserPrincipal principal) {
        adminEmpresaIdOrThrow(principal);

        String extension = imagenUploadValidator.validarYObtenerExtension(file);
        String nombreArchivo = "bloque-" + UUID.randomUUID() + extension;

        try {
            Files.createDirectories(UPLOAD_DIR);
            file.transferTo(UPLOAD_DIR.resolve(nombreArchivo));
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la imagen", ex);
        }
        return new BloqueImagenUploadDto("/uploads/bloques/" + nombreArchivo);
    }

    // --- validaciones ---

    private void validarSlotYAncho(GuardarTiendaBloqueRequest request) {
        if (request.slot() == null || !TiendaBloqueCatalogo.SLOTS.contains(request.slot())) {
            throw new BloqueTiendaInvalidoException("Slot inválido");
        }
        if (request.ancho() == null || !TiendaBloqueCatalogo.ANCHOS.contains(request.ancho())) {
            throw new BloqueTiendaInvalidoException("Ancho inválido");
        }
    }

    // Valida y aplica el request a la card (compartido por crear y actualizar).
    private void aplicar(TiendaBloqueCard card, GuardarTiendaBloqueCardRequest request, Long empresaId) {
        String imagen = request.imagenUrl() == null ? null : request.imagenUrl().trim();
        // La imagen sale de POST /imagenes: solo se aceptan rutas propias, sin recorrer directorios.
        if (imagen == null || !imagen.startsWith("/uploads/") || imagen.contains("..")) {
            throw new BloqueTiendaInvalidoException("La imagen es obligatoria y debe subirse desde el panel");
        }
        String orientacion = request.orientacion() == null ? "VERTICAL" : request.orientacion();
        if (!TiendaBloqueCatalogo.ORIENTACIONES.contains(orientacion)) {
            throw new BloqueTiendaInvalidoException("Orientación inválida");
        }
        String accion = request.accion() == null ? "NINGUNA" : request.accion();
        if (!TiendaBloqueCatalogo.ACCIONES.contains(accion)) {
            throw new BloqueTiendaInvalidoException("Acción inválida");
        }
        String titulo = limpiar(request.titulo());
        String texto = limpiar(request.texto());

        String valor = switch (accion) {
            case "URL" -> validarUrl(request.accionValor());
            case "PRODUCTO" -> validarProducto(request.accionValor(), empresaId);
            case "CATEGORIA" -> validarCategoria(request.accionValor(), empresaId);
            case "MODAL" -> {
                if (titulo == null && texto == null) {
                    throw new BloqueTiendaInvalidoException("La acción MODAL requiere título o texto en la card");
                }
                yield null;
            }
            default -> null; // NINGUNA: se ignora/limpia el valor
        };

        card.setImagenUrl(imagen);
        card.setOrientacion(orientacion);
        card.setTitulo(titulo);
        card.setTexto(texto);
        card.setAccion(accion);
        card.setAccionValor(valor);
    }

    private String validarUrl(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new BloqueTiendaInvalidoException("La URL es obligatoria");
        }
        String url = valor.trim();
        try {
            URI uri = new URI(url);
            String esquema = uri.getScheme();
            boolean http = "http".equalsIgnoreCase(esquema) || "https".equalsIgnoreCase(esquema);
            if (!http || uri.getHost() == null) {
                throw new BloqueTiendaInvalidoException("La URL debe ser absoluta y empezar con http:// o https://");
            }
        } catch (URISyntaxException ex) {
            throw new BloqueTiendaInvalidoException("La URL no es válida");
        }
        return url;
    }

    private String validarProducto(String valor, Long empresaId) {
        Long id = parsearId(valor, "producto");
        if (productoRepository.findByIdAndEmpresaId(id, empresaId).isEmpty()) {
            throw new BloqueTiendaInvalidoException("El producto no existe en esta empresa");
        }
        return String.valueOf(id);
    }

    private String validarCategoria(String valor, Long empresaId) {
        Long id = parsearId(valor, "categoría");
        if (categoriaRepository.findByIdAndEmpresaId(id, empresaId).isEmpty()) {
            throw new BloqueTiendaInvalidoException("La categoría no existe en esta empresa");
        }
        return String.valueOf(id);
    }

    private Long parsearId(String valor, String entidad) {
        if (valor == null || valor.isBlank()) {
            throw new BloqueTiendaInvalidoException("Falta el " + entidad + " de destino");
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            throw new BloqueTiendaInvalidoException("El " + entidad + " de destino no es válido");
        }
    }

    // --- helpers ---

    private TiendaBloque bloqueDeLaEmpresa(Long id, Long empresaId) {
        return bloqueRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(BloqueTiendaNoEncontradoException::new);
    }

    // Mismo criterio que TiendaTipServiceImpl.adminEmpresaIdOrThrow.
    private Long adminEmpresaIdOrThrow(UserPrincipal principal) {
        if (principal == null || principal.empresaId() == null) {
            throw new SinEmpresaException();
        }
        if (principal.rolEmpresa() != RolEmpresa.ADMIN) {
            throw new AccesoRestringidoAdminException();
        }
        return principal.empresaId();
    }

    private String limpiar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    // Una query para las cards de todos los bloques (sin N+1).
    private List<TiendaBloqueDto> conCards(List<TiendaBloque> bloques) {
        if (bloques.isEmpty()) {
            return List.of();
        }
        List<Long> ids = bloques.stream().map(TiendaBloque::getId).toList();
        Map<Long, List<TiendaBloqueCardDto>> cardsPorBloque = cardRepository.findByBloqueIdInOrderByOrdenAscIdAsc(ids).stream()
                .collect(Collectors.groupingBy(TiendaBloqueCard::getBloqueId,
                        Collectors.mapping(this::toCardDto, Collectors.toList())));
        return bloques.stream()
                .map(b -> toDto(b, cardsPorBloque.getOrDefault(b.getId(), List.of())))
                .toList();
    }

    private TiendaBloqueDto toDtoConCards(TiendaBloque bloque) {
        List<TiendaBloqueCardDto> cards = cardRepository.findByBloqueIdOrderByOrdenAscIdAsc(bloque.getId()).stream()
                .map(this::toCardDto)
                .toList();
        return toDto(bloque, cards);
    }

    private TiendaBloqueDto toDto(TiendaBloque b, List<TiendaBloqueCardDto> cards) {
        return new TiendaBloqueDto(b.getId(), b.getTitulo(), b.getSlot(), b.getAncho(), b.getOrden(), b.getActivo(), cards);
    }

    private TiendaBloqueCardDto toCardDto(TiendaBloqueCard c) {
        return new TiendaBloqueCardDto(c.getId(), c.getImagenUrl(), c.getOrientacion(), c.getTitulo(), c.getTexto(),
                c.getOrden(), c.getAccion(), c.getAccionValor());
    }
}
