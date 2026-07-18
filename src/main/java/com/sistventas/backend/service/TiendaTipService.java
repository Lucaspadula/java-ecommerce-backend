package com.sistventas.backend.service;

import com.sistventas.backend.dto.CrearTipRequest;
import com.sistventas.backend.dto.TipDto;
import com.sistventas.backend.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Solo ADMIN (ver adminEmpresaIdOrThrow en la impl), mismo criterio que
// TiendaTestimonioService: la personalización de la tienda pública no la
// puede hacer un MEMBER.
public interface TiendaTipService {
    List<TipDto> listar(UserPrincipal principal);

    TipDto crear(CrearTipRequest request, UserPrincipal principal);

    void eliminar(Long id, UserPrincipal principal);

    // Foto opcional del tip, subida aparte (mismo patrón multipart que
    // TiendaTestimonioService.actualizarFoto).
    TipDto actualizarFoto(Long id, MultipartFile file, UserPrincipal principal);
}
