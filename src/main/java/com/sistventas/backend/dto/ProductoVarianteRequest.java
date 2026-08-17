package com.sistventas.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Línea de variante de color enviada al crear/editar un producto. id
// nullable a propósito: null en una variante nueva (todavía no existe la
// fila); si viene cargado, ProductoServiceImpl actualiza la fila EXISTENTE
// en vez de recrearla — a diferencia de una línea de receta (ProductoInsumo),
// esta fila es la fuente de stock que VentaServiceImpl referencia directo
// vía VentaItem.varianteId, así que recrearla en cada edición dejaría una
// venta ya cargada apuntando a una fila borrada.
//
// Sin precioVenta a propósito: existió un override de precio opcional por
// color, se sacó porque generaba confusión (el margen mostrado en el form
// es siempre del producto entero, nunca por color). Si un color necesita
// otro precio, se carga como un Producto nuevo. La columna precio_venta de
// producto_variante queda en la base sin usarse, mismo criterio que
// Producto.stock (no vale la pena una migración solo para borrarla).
public record ProductoVarianteRequest(
        Long id,

        @NotBlank(message = "El color es obligatorio")
        String color,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        String fotoUrl
) {}
