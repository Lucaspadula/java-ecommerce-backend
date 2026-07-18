package com.sistventas.backend.service;

import com.sistventas.backend.security.UserPrincipal;

/**
 * Exportación del catálogo de productos activos de la empresa (PDF/Excel).
 * Mismo scoping multiempresa que ProductoService: siempre a partir del
 * empresaId resuelto del UserPrincipal, nunca de un valor del cliente.
 */
public interface CatalogoService {
    byte[] generarPdf(UserPrincipal principal);

    byte[] generarExcel(UserPrincipal principal);
}
