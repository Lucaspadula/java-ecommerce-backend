package com.sistventas.backend.dto;

import java.math.BigDecimal;

// DTO admin (listado + resultado de crear/actualizar). Cada lado viaja con
// su id (para el form de edición, que reusa los mismos <select> que el alta)
// y su nombre ya resuelto (para mostrarlo en la lista sin otro round-trip).
public record ReglaDescuentoComboDto(
        Long id,
        Long categoriaAId,
        String categoriaANombre,
        Long subcategoriaAId,
        String subcategoriaANombre,
        Long categoriaBId,
        String categoriaBNombre,
        Long subcategoriaBId,
        String subcategoriaBNombre,
        BigDecimal porcentaje,
        boolean activo
) {}
