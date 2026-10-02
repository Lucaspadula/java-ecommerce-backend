package com.sistventas.backend.dto;

import jakarta.validation.constraints.Size;

// PUT /api/empresa/tienda/apariencia: textos del banner (hero), tipografía y
// tema. Solo estos campos; el resto de la tienda no se toca (nav R5).
public record ActualizarAparienciaRequest(
        @Size(max = 100, message = "El subtítulo no puede superar los 100 caracteres")
        String tiendaBannerTagline,

        @Size(max = 150, message = "El título no puede superar los 150 caracteres")
        String tiendaBannerTitulo,

        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String tiendaBannerDescripcion,

        // Null/vacío => "clasica".
        @Size(max = 20, message = "La tipografía no puede superar los 20 caracteres")
        String tiendaFuente,

        // Null/vacío => "claro".
        @Size(max = 20, message = "El tema no puede superar los 20 caracteres")
        String tiendaTema
) {}
