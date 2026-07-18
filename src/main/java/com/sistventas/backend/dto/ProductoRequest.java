package com.sistventas.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ProductoRequest(
        @NotBlank(message = "El nombre del producto es obligatorio")
        String nombre,

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        // Opcional: no todo producto tiene subcategoría.
        Long subcategoriaId,

        String descripcion,

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio de venta no puede ser negativo")
        BigDecimal precioVenta,

        // Ambos opcionales: la venta por mayor solo se activa si los dos
        // están cargados (ver VentaServiceImpl.precioSegunCantidad).
        @DecimalMin(value = "0.0", message = "El precio por mayor no puede ser negativo")
        BigDecimal precioPorMayor,

        @Min(value = 1, message = "La cantidad mínima para mayorista debe ser al menos 1")
        Integer cantidadMinimaMayorista,

        // Todo producto se compone de insumos, sin excepción — incluso uno
        // comprado ya hecho a un tercero se carga como un solo insumo que
        // representa el producto entero, con receta de una línea (cantidad
        // 1). No hay stock ni costo que se carguen a mano: ambos salen
        // siempre de la receta (ver ProductoServiceImpl).
        @NotEmpty(message = "El producto necesita al menos un artículo")
        @Valid
        List<ProductoInsumoRequest> insumos
) {}
