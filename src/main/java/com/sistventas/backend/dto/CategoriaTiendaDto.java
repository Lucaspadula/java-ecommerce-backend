package com.sistventas.backend.dto;

// color/imagenUrl vienen null cuando la categoría todavía no fue
// personalizada — el frontend público ya tiene un fallback por hash para ese
// caso, no lo dupliques acá. categoriaId identifica la categoría real (ver
// entity Categoria): los endpoints de escritura (actualizarColor/Imagen)
// referencian por este id, no por nombre — dos categorías nunca pueden
// colisionar por texto (mayúsculas, espacios) como pasaba antes del
// refactor a tabla maestra.
public record CategoriaTiendaDto(
        Long categoriaId,
        String nombre,
        String color,
        String imagenUrl
) {}
