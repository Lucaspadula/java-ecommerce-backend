package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// A diferencia de CrearCatalogoSeccionRequest, este no lleva `tipo`: el tipo
// de un punto ya creado no cambia, solo su texto/estilo (ver
// TiendaCatalogoSeccionServiceImpl.actualizar). Antes de esto no existía
// forma de editar un punto ya guardado — solo crear uno nuevo o borrarlo.
public record ActualizarCatalogoSeccionRequest(
        @NotBlank(message = "El texto es obligatorio")
        @Size(max = 2000, message = "El texto no puede superar los 2000 caracteres")
        String texto,

        @Pattern(regexp = "^(IZQUIERDA|CENTRO|DERECHA)?$", message = "La alineación debe ser IZQUIERDA, CENTRO o DERECHA")
        String alineacion,
        Boolean negrita,
        Boolean cursiva,
        Boolean subrayado
) {}
