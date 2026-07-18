package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.CrearTestimonioRequest;
import com.sistventas.backend.dto.TestimonioDto;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.TiendaTestimonio;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.exception.TestimonioNoEncontradoException;
import com.sistventas.backend.repository.TiendaTestimonioRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.TiendaTestimonioService;
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
 * Testimonios generales del negocio (nombre + comentario, con foto y canal
 * opcionales, sin producto asociado), cargados a mano por el dueño para la
 * franja fija de la home de la tienda pública. Separado de ResenaService (que
 * vive dentro de ProductoServiceImpl) porque una reseña SIEMPRE está ligada a
 * un producto, mientras que un testimonio es de la empresa en general.
 */
@Service
public class TiendaTestimonioServiceImpl implements TiendaTestimonioService {

    // Directorio propio (no uploads/resenas): la foto de un testimonio
    // general no está ligada a un producto, mismo criterio de separación de
    // ciclos de vida que ProductoServiceImpl.UPLOAD_DIR_RESENAS.
    private static final Path UPLOAD_DIR = Paths.get("uploads", "testimonios");

    private final TiendaTestimonioRepository tiendaTestimonioRepository;
    private final ImagenUploadValidator imagenUploadValidator;

    public TiendaTestimonioServiceImpl(TiendaTestimonioRepository tiendaTestimonioRepository,
                                        ImagenUploadValidator imagenUploadValidator) {
        this.tiendaTestimonioRepository = tiendaTestimonioRepository;
        this.imagenUploadValidator = imagenUploadValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestimonioDto> listar(UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        return tiendaTestimonioRepository.findByEmpresaIdOrderByOrdenAscIdAsc(empresaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public TestimonioDto crear(CrearTestimonioRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);

        TiendaTestimonio testimonio = new TiendaTestimonio();
        testimonio.setEmpresaId(empresaId);
        testimonio.setClienteNombre(request.clienteNombre().trim());
        testimonio.setComentario(request.comentario().trim());
        testimonio.setCanal(request.canal());
        // Se agrega siempre al final de la franja: mismo criterio que
        // PerfilServiceImpl.agregarBannerImagen (orden = cantidad actual).
        testimonio.setOrden((int) tiendaTestimonioRepository.countByEmpresaId(empresaId));

        return toDto(tiendaTestimonioRepository.save(testimonio));
    }

    @Override
    @Transactional
    public void eliminar(Long id, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        TiendaTestimonio testimonio = tiendaTestimonioRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(TestimonioNoEncontradoException::new);
        tiendaTestimonioRepository.delete(testimonio);
    }

    @Override
    @Transactional
    public TestimonioDto actualizarFoto(Long id, MultipartFile file, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        TiendaTestimonio testimonio = tiendaTestimonioRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(TestimonioNoEncontradoException::new);

        String extension = imagenUploadValidator.validarYObtenerExtension(file);
        String nombreArchivo = "testimonio-" + testimonio.getId() + "-" + UUID.randomUUID() + extension;

        try {
            Files.createDirectories(UPLOAD_DIR);
            Path destino = UPLOAD_DIR.resolve(nombreArchivo);
            file.transferTo(destino);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la imagen", ex);
        }

        testimonio.setFotoUrl("/uploads/testimonios/" + nombreArchivo);
        return toDto(tiendaTestimonioRepository.save(testimonio));
    }

    // Mismo criterio que TiendaCategoriaServiceImpl.adminEmpresaIdOrThrow: la
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

    private TestimonioDto toDto(TiendaTestimonio testimonio) {
        return new TestimonioDto(testimonio.getId(), testimonio.getClienteNombre(), testimonio.getComentario(),
                testimonio.getOrden(), testimonio.getFotoUrl(), testimonio.getCanal());
    }
}
