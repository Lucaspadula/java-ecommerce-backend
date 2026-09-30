package com.sistventas.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarCatalogoConfigRequest(
        boolean mostrarLogo,

        // Opcional: null/vacío = la portada usa el nombre de la empresa (ver
        // PerfilServiceImpl.actualizarCatalogoConfig).
        @Size(max = 150, message = "El título no puede superar los 150 caracteres")
        String tituloPersonalizado,

        boolean mostrarDescripcion,
        boolean mostrarColores,

        // Opcional: null/vacío = fondo blanco de siempre en las páginas de
        // productos. Formato hex ("#rrggbb"), validado también en
        // PerfilServiceImpl antes de guardar.
        @Pattern(regexp = "^(#[0-9a-fA-F]{6})?$", message = "El color debe tener formato hex, ej. #f7f3ec")
        String colorFondoProductos
) {}
