package com.sistventas.backend.service;

import com.sistventas.backend.dto.ActualizarCatalogoSeccionRequest;
import com.sistventas.backend.dto.CatalogoSeccionDto;
import com.sistventas.backend.dto.CrearCatalogoSeccionRequest;
import com.sistventas.backend.security.UserPrincipal;

import java.util.List;

// Solo ADMIN (ver adminEmpresaIdOrThrow en la impl), mismo criterio que
// TiendaTipService: la personalización del catálogo no la puede hacer un
// MEMBER.
public interface TiendaCatalogoSeccionService {
    List<CatalogoSeccionDto> listar(UserPrincipal principal);

    CatalogoSeccionDto crear(CrearCatalogoSeccionRequest request, UserPrincipal principal);

    // Antes de V58 no existía forma de editar un punto ya creado (solo
    // crear uno nuevo o borrarlo) — hace falta para poder tocar el
    // texto/estilo de un punto sin perder su `orden`.
    CatalogoSeccionDto actualizar(Long id, ActualizarCatalogoSeccionRequest request, UserPrincipal principal);

    void eliminar(Long id, UserPrincipal principal);
}
