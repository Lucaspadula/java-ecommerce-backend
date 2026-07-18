package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Find-or-create (ver CategoriaServiceImpl.crear): si ya existe una
// categoría con este nombre (case-insensitive) para la empresa, se devuelve
// esa en vez de crear un duplicado — dos usuarios/formularios escribiendo
// "Mates" y "mates" terminan apuntando a la misma fila.
public record CrearCategoriaRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre
) {}
