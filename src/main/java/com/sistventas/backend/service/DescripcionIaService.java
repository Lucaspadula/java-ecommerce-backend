package com.sistventas.backend.service;

import com.sistventas.backend.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;

public interface DescripcionIaService {

    /**
     * Genera una descripción corta a partir de una foto de producto. Usa la
     * key propia de la empresa de `principal` si la cargó en Configuración;
     * si no, cae a la key global (sistventas.gemini.api-key).
     * @param nombreProducto opcional — si viene, se lo suma al prompt como contexto.
     */
    String generar(MultipartFile foto, String nombreProducto, UserPrincipal principal);
}
