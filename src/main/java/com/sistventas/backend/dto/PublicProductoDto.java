package com.sistventas.backend.dto;

import java.math.BigDecimal;
import java.util.List;

// DTO público de vidriera: nunca incluye costoUnitario, insumos ni
// precioPorMayor (Fase 1 usa precio único, sin exponer datos de costo).
public record PublicProductoDto(
        Long id,
        String nombre,
        String descripcion,
        String categoria,
        BigDecimal precioVenta,
        String fotoUrl,
        // Slots 2 y 3 de la galería (nullable): solo se usan en la fila de
        // miniaturas del modal de detalle de la tienda pública, nunca en
        // grilla/destacados/relacionados (ver Producto.fotoUrl2/3).
        String fotoUrl2,
        String fotoUrl3,
        Integer stock,
        // null si el producto no tiene ninguna reseña cargada. Ver
        // ResenaDestacadaDto.
        ResenaDestacadaDto resenaDestacada,
        // Vacía = producto simple, sin selector de color en la tienda
        // (comportamiento de siempre). Ver PublicVarianteDto.
        List<PublicVarianteDto> variantes
) {}
