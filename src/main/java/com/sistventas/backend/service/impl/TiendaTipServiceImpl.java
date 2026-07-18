package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.CrearTipRequest;
import com.sistventas.backend.dto.TipDto;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.TiendaTip;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.exception.TipNoEncontradoException;
import com.sistventas.backend.repository.TiendaTipRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.TiendaTipService;
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

/**
 * Tips de cuidado del mate (título + contenido, con foto opcional, sin
 * autor), cargados a mano por el dueño para la franja fija de la home de la
 * tienda pública, mostrada después de los testimonios. Mismo criterio que
 * TiendaTestimonioServiceImpl, solo que acá no hay una persona asociada al
 * contenido.
 */
@Service
public class TiendaTipServiceImpl implements TiendaTipService {

    // Directorio propio, mismo criterio que TiendaTestimonioServiceImpl.UPLOAD_DIR.
    private static final Path UPLOAD_DIR = Paths.get("uploads", "tips");

    private final TiendaTipRepository tiendaTipRepository;
    private final ImagenUploadValidator imagenUploadValidator;

    public TiendaTipServiceImpl(TiendaTipRepository tiendaTipRepository,
                                 ImagenUploadValidator imagenUploadValidator) {
        this.tiendaTipRepository = tiendaTipRepository;
        this.imagenUploadValidator = imagenUploadValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipDto> listar(UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        return tiendaTipRepository.findByEmpresaIdOrderByOrdenAscIdAsc(empresaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public TipDto crear(CrearTipRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);

        TiendaTip tip = new TiendaTip();
        tip.setEmpresaId(empresaId);
        tip.setTitulo(request.titulo().trim());
        tip.setContenido(request.contenido().trim());
        // Se agrega siempre al final de la franja: mismo criterio que
        // TiendaTestimonioServiceImpl.crear (orden = cantidad actual).
        tip.setOrden((int) tiendaTipRepository.countByEmpresaId(empresaId));

        return toDto(tiendaTipRepository.save(tip));
    }

    @Override
    @Transactional
    public void eliminar(Long id, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        TiendaTip tip = tiendaTipRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(TipNoEncontradoException::new);
        tiendaTipRepository.delete(tip);
    }

    @Override
    @Transactional
    public TipDto actualizarFoto(Long id, MultipartFile file, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        TiendaTip tip = tiendaTipRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(TipNoEncontradoException::new);

        String extension = imagenUploadValidator.validarYObtenerExtension(file);
        String nombreArchivo = "tip-" + tip.getId() + "-" + UUID.randomUUID() + extension;

        try {
            Files.createDirectories(UPLOAD_DIR);
            Path destino = UPLOAD_DIR.resolve(nombreArchivo);
            file.transferTo(destino);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la imagen", ex);
        }

        tip.setFotoUrl("/uploads/tips/" + nombreArchivo);
        return toDto(tiendaTipRepository.save(tip));
    }

    // Mismo criterio que TiendaTestimonioServiceImpl.adminEmpresaIdOrThrow: la
    // personalización de la tienda pública solo la puede hacer el ADMIN.
    private Long adminEmpresaIdOrThrow(UserPrincipal principal) {
        if (principal == null || principal.empresaId() == null) {
            throw new SinEmpresaException();
        }
        if (principal.rolEmpresa() != RolEmpresa.ADMIN) {
            throw new AccesoRestringidoAdminException();
        }
        return principal.empresaId();
    }

    private TipDto toDto(TiendaTip tip) {
        return new TipDto(tip.getId(), tip.getTitulo(), tip.getContenido(), tip.getOrden(), tip.getFotoUrl());
    }
}
