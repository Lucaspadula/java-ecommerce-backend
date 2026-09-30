package com.sistventas.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

// Estilo GLOBAL del texto de "Importante"/"Cómo comprar" (ver plan
// acordado: un único estilo para toda la sección, no por bloque). Los 3
// campos son opcionales: null/vacío = vuelve al default de siempre
// (Helvetica, 10pt, gris oscuro) — ver PerfilServiceImpl.actualizarEstiloTextoCatalogo.
public record ActualizarEstiloTextoCatalogoRequest(
        // Valores válidos: "helvetica", "times", "courier" — los mismos 3
        // tipos base que trae OpenPDF sin embeber fuentes.
        @Pattern(regexp = "^(helvetica|times|courier)?$", message = "Tipografía inválida")
        String fuente,

        @Min(value = 8, message = "El tamaño mínimo es 8")
        @Max(value = 18, message = "El tamaño máximo es 18")
        Integer tamanio,

        @Pattern(regexp = "^(#[0-9a-fA-F]{6})?$", message = "El color debe tener formato hex, ej. #333333")
        String color
) {}
