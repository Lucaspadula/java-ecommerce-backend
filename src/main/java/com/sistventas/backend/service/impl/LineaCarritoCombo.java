package com.sistventas.backend.service.impl;

import java.math.BigDecimal;

// Input del motor de cálculo (CalculadorDescuentoComboService): un producto +
// cantidad + precio unitario snapshot + su categoría/subcategoría reales.
// Se arma SIEMPRE server-side (checkout público y preview), resolviendo cada
// productoId contra Producto persistido — nunca a partir de precio/categoría
// que venga del cliente. El matcheo contra ReglaDescuentoCombo es por id
// (categoriaId/subcategoriaId); los nombres viajan aparte SOLO para armar el
// texto legible del descuento aplicado ("Mates + Bombillas"), nunca se usan
// para comparar.
public record LineaCarritoCombo(
        Long productoId,
        int cantidad,
        BigDecimal precioUnitario,
        Long categoriaId,
        String categoriaNombre,
        Long subcategoriaId,
        String subcategoriaNombre
) {}
