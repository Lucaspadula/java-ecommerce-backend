package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearCatalogoSeccionRequest(
        // Valores válidos: "IMPORTANTE", "COMO_COMPRAR" (ver
        // TiendaCatalogoSeccionServiceImpl.tipoValidoOThrow).
        @NotBlank(message = "El tipo es obligatorio")
        String tipo,

        @NotBlank(message = "El texto es obligatorio")
        @Size(max = 2000, message = "El texto no puede superar los 2000 caracteres")
        String texto,

        // Estilo por bloque (V58), todos opcionales — null/vacío cae al
        // default de siempre (izquierda, sin negrita/cursiva/subrayado) en
        // TiendaCatalogoSeccionServiceImpl.
        @Pattern(regexp = "^(IZQUIERDA|CENTRO|DERECHA)?$", message = "La alineación debe ser IZQUIERDA, CENTRO o DERECHA")
        String alineacion,
        Boolean negrita,
        Boolean cursiva,
        Boolean subrayado
) {}
