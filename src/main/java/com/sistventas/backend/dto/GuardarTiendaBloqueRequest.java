package com.sistventas.backend.dto;

import jakarta.validation.constraints.Size;

// slot y ancho se validan contra TiendaBloqueCatalogo en el service (400).
// activo null: en crear queda true; en actualizar se conserva el actual.
public record GuardarTiendaBloqueRequest(
        @Size(max = 150, message = "El título no puede superar los 150 caracteres")
        String titulo,
        String slot,
        String ancho,
        Boolean activo
) {}
