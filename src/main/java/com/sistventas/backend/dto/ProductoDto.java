package com.sistventas.backend.dto;

import java.math.BigDecimal;
import java.util.List;

// categoria/subcategoria viajan como STRING (el nombre resuelto), no como
// id: Producto.categoria/subcategoria pasaron a ser @ManyToOne a
// Categoria/Subcategoria (tabla maestra, ver entity Producto), pero la
// normalización es interna — la superficie de esta API sigue viéndose igual
// que antes del refactor para no romper de más al frontend que solo
// necesita MOSTRAR la categoría, nunca su id (a diferencia de
// ProductoRequest/ActualizarProductoRequest, que sí reciben categoriaId
// porque ahí el cliente elige de una lista real).
public record ProductoDto(
        Long id,
        String nombre,
        String categoria,
        String subcategoria,
        String descripcion,
        BigDecimal precioVenta,
        BigDecimal precioPorMayor,
        Integer cantidadMinimaMayorista,
        String fotoUrl,
        // Slots 2 y 3 de la galería (nullable): solo se usan en el modal de
        // detalle del admin, nunca en el listado/grilla (ver Producto.fotoUrl2/3).
        String fotoUrl2,
        String fotoUrl3,
        Integer stock,
        // Calculado, no persistido: siempre la suma de insumos[].subtotal —
        // centraliza en el backend un cálculo que antes hacía el frontend.
        BigDecimal costoUnitario,
        List<ProductoInsumoDto> insumos,
        // Variantes de color (Etapa 1): lista vacía = producto simple, stock
        // propio de siempre (ver Producto.variantes).
        List<ProductoVarianteDto> variantes
) {}
