package com.sistventas.backend.dto;

import java.math.BigDecimal;

// Línea de receta: un Insumo maestro + cuánto de él usa el producto.
// costoUnitario y subtotal se calculan al vuelo a partir del Insumo
// vinculado (costoUnitario * cantidad) — no se persisten, para que siempre
// reflejen el costo actual del insumo maestro.
public record ProductoInsumoDto(
        Long id,
        Long insumoId,
        String insumoNombre,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal subtotal
) {}
