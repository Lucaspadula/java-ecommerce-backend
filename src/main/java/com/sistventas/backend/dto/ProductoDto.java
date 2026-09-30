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
        // Derivado (ProductoFotoResolver.resolverMiniatura sobre `fotos`), no
        // una columna propia — ver Producto.fotoUrl (deprecada). Se conserva
        // como escalar para no romper el listado/grilla, que solo necesita
        // UNA miniatura.
        String fotoUrl,
        // Pool unificado de fotos (V43__producto_foto_pool.sql), reemplaza
        // fotoUrl2/3/4: hasta 8 por producto, cada una con varianteId
        // opcional (color) — ver ProductoFotoDto.
        List<ProductoFotoDto> fotos,
        Integer stock,
        // Calculado (ver ProductoServiceImpl.costoEfectivo): costoPropio +
        // suma de insumos[].subtotal. Para MOSTRAR costo/margen y para
        // costear este producto como componente de un kit — NO usar para
        // repoblar el form de edición (ver costoPropio).
        BigDecimal costoUnitario,
        // Persistido tal cual lo cargó el usuario (Producto.costoUnitario en
        // el entity) — sin sumarle los insumos. Es el valor que hay que usar
        // para repoblar el campo "Costo propio" al editar un producto; usar
        // `costoUnitario` de arriba ahí duplicaría el costo de los insumos.
        BigDecimal costoPropio,
        List<ProductoInsumoDto> insumos,
        // Variantes de color (Etapa 1): lista vacía = producto simple, stock
        // propio de siempre (ver Producto.variantes).
        List<ProductoVarianteDto> variantes,
        // Kit (Composite): lista vacía = producto simple, de siempre (ver
        // Producto.componentes).
        List<ProductoComponenteDto> componentes,
        // Lugares grabables: lista vacía = producto sin opción de grabado
        // (ver Producto.grabados).
        List<ProductoGrabadoDto> grabados,
        // Valores de atributo de filtro asignados (ej. Material = "Acero") —
        // lista vacía = sin ningún atributo asignado (ver Producto.atributoValores).
        List<AtributoFiltroValorDto> atributoValores
) {}
