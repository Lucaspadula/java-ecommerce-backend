package com.sistventas.backend.service;

import com.sistventas.backend.dto.BloqueImagenUploadDto;
import com.sistventas.backend.dto.GuardarTiendaBloqueCardRequest;
import com.sistventas.backend.dto.GuardarTiendaBloqueRequest;
import com.sistventas.backend.dto.ReordenarBloquesRequest;
import com.sistventas.backend.dto.ReordenarCardsRequest;
import com.sistventas.backend.dto.TiendaBloqueCardDto;
import com.sistventas.backend.dto.TiendaBloqueDto;
import com.sistventas.backend.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Administración de los bloques configurables de la tienda pública. Solo
 * ADMIN de la empresa; la empresa sale siempre del principal, nunca del body.
 */
public interface TiendaBloqueService {

    List<TiendaBloqueDto> listar(UserPrincipal principal);

    TiendaBloqueDto crear(GuardarTiendaBloqueRequest request, UserPrincipal principal);

    TiendaBloqueDto actualizar(Long id, GuardarTiendaBloqueRequest request, UserPrincipal principal);

    void eliminar(Long id, UserPrincipal principal);

    TiendaBloqueDto actualizarActivo(Long id, boolean activo, UserPrincipal principal);

    List<TiendaBloqueDto> reordenar(ReordenarBloquesRequest request, UserPrincipal principal);

    TiendaBloqueCardDto crearCard(Long bloqueId, GuardarTiendaBloqueCardRequest request, UserPrincipal principal);

    TiendaBloqueCardDto actualizarCard(Long bloqueId, Long cardId, GuardarTiendaBloqueCardRequest request,
                                       UserPrincipal principal);

    void eliminarCard(Long bloqueId, Long cardId, UserPrincipal principal);

    List<TiendaBloqueCardDto> reordenarCards(Long bloqueId, ReordenarCardsRequest request, UserPrincipal principal);

    BloqueImagenUploadDto subirImagen(MultipartFile file, UserPrincipal principal);
}
