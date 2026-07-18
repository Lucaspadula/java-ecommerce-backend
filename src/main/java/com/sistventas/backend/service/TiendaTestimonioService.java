package com.sistventas.backend.service;

import com.sistventas.backend.dto.CrearTestimonioRequest;
import com.sistventas.backend.dto.TestimonioDto;
import com.sistventas.backend.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Solo ADMIN (ver adminEmpresaIdOrThrow en la impl), mismo criterio que
// TiendaCategoriaService: la personalización de la tienda pública no la
// puede hacer un MEMBER.
public interface TiendaTestimonioService {
    List<TestimonioDto> listar(UserPrincipal principal);

    TestimonioDto crear(CrearTestimonioRequest request, UserPrincipal principal);

    void eliminar(Long id, UserPrincipal principal);

    // Foto opcional del testimonio, subida aparte (mismo patrón multipart
    // que ProductoServiceImpl.actualizarFotoResena).
    TestimonioDto actualizarFoto(Long id, MultipartFile file, UserPrincipal principal);
}
