package com.sistventas.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

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
//
// Sin fotoUrl a propósito (sacado en producto-fotos-pool): la foto de una
// variante ahora vive en producto_foto (ver ProductoFotoResolver), nunca en
// este payload. La columna producto_variante.foto_url queda deprecated en la
// base como red de seguridad de rollback — NO se debe volver a escribir
// desde acá, eso fue justamente el bug que este comentario reemplaza
// (ProductoServiceImpl.aplicarVariantes pisaba la columna con null en cada
// guardado porque este campo dejó de mandarse).
public record ProductoVarianteRequest(
        Long id,

        @NotBlank(message = "El color es obligatorio")
        String color,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        // Hex real del color elegido en el admin (ej. "#c39a8f"), opcional:
        // variantes viejas pueden no tenerlo cargado. Formato #RGB o #RRGGBB,
        // igual que el <input type="color"> nativo del browser.
        @Pattern(regexp = "^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$", message = "El color debe ser un hex válido (ej: #c39a8f)")
        String colorHex
) {}
