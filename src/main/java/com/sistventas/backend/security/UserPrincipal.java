package com.sistventas.backend.security;

import com.sistventas.backend.entity.RolEmpresa;

/**
 * Principal autenticado que viaja en el SecurityContext para toda request con
 * JWT válido. Se arma una sola vez en JwtAuthenticationFilter a partir de los
 * claims del token, así ningún controller necesita volver a parsear el JWT
 * para saber a qué empresa pertenece el usuario logueado.
 *
 * empresaId es null para el Super Admin (no tiene empresa asociada) — los
 * endpoints que requieren empresa deben rechazar ese caso explícitamente
 * (ver SinEmpresaException), nunca asumir que empresaId viene seteado.
 */
public record UserPrincipal(
        Long id,
        Long empresaId,
        boolean esSuperAdmin,
        RolEmpresa rolEmpresa
) {}
