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
        // null si el producto no tiene subcategoría asignada (es opcional).
        String subcategoria,
        // Ids normalizados (post-3FN), no solo el nombre: los usa el frontend
        // para armar el carrusel de "similares" sin comparar por texto.
        // subcategoriaId nullable porque la subcategoría en sí es opcional.
        Long categoriaId,
        Long subcategoriaId,
        BigDecimal precioVenta,
        // Derivado (ProductoFotoResolver.resolverMiniatura), usado en
        // grilla/destacados/relacionados — ver Producto.fotoUrl (deprecada).
        String fotoUrl,
        // Pool unificado de fotos (V43__producto_foto_pool.sql), reemplaza
        // fotoUrl2/3/4: se usa para la fila de miniaturas del modal de
        // detalle (ver spec "Contrato público de fotos como lista").
        List<ProductoFotoDto> fotos,
        Integer stock,
        // null si el producto no tiene ninguna reseña cargada. Ver
        // ResenaDestacadaDto.
        ResenaDestacadaDto resenaDestacada,
        // Promedio de puntuacion (1-5) y cantidad de reseñas verificadas
        // (ventaId no null) del producto — null/0 si todavía no tiene
        // ninguna, para no inventar un rating de 0 estrellas en la card.
        Double promedioResenas,
        Integer cantidadResenas,
        // Vacía = producto simple, sin selector de color en la tienda
        // (comportamiento de siempre). Ver PublicVarianteDto.
        List<PublicVarianteDto> variantes,
        // Vacía = producto simple, no es un kit. Si tiene, la tienda muestra
        // "Incluye: X + Y" en el modal de detalle — ver PublicComponenteDto.
        List<PublicComponenteDto> componentes,
        // Vacía = sin opción de grabado. A diferencia de PublicComponenteDto,
        // acá SÍ se expone el precio (ProductoGrabadoDto completo): es un
        // servicio adicional que el cliente compra, no un costo interno.
        List<ProductoGrabadoDto> grabados,
        // Valores de atributo de filtro asignados (ej. Material = "Acero") —
        // vacía = sin ninguno. La pantalla de categoría arma sus chips de
        // filtro agrupando esto por atributo, sin pedirle nada aparte al
        // backend (ver TiendaCategoria en el frontend).
        List<AtributoFiltroValorDto> atributoValores
) {}
