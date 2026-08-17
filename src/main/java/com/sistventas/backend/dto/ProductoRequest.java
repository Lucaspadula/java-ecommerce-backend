package com.sistventas.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

        // Ya no es @NotEmpty acá: un producto puede componerse de insumos, de
        // componentes (kit), o de ninguno de los dos (producto simple con
        // stock propio, ver `stock`/`costoUnitario` más abajo).
        @Valid
        List<ProductoInsumoRequest> insumos,

        // Opcional: no todo producto tiene variantes de color. Lista vacía o
        // null = producto simple, stock propio de siempre (ver
        // ProductoServiceImpl y StockDisponibleCalculator, que ramifican
        // explícitamente en este chequeo antes de tocar el camino existente).
        @Valid
        List<ProductoVarianteRequest> variantes,

        // Kit (Composite): lista vacía o null = producto simple, de siempre.
        // Cada línea referencia OTRO Producto ya cargado + cantidad — ver
        // ProductoComponente y StockDisponibleCalculator.calcularKit.
        @Valid
        List<ProductoComponenteRequest> componentes,

        // Lugares grabables: lista vacía o null = producto sin opción de
        // grabado, no se muestra el selector en la tienda pública (ver
        // ProductoGrabado).
        @Valid
        List<ProductoGrabadoRequest> grabados,

        // Ambos SOLO se usan cuando el producto no tiene insumos ni
        // componentes (producto simple, stock propio) — ver
        // StockDisponibleCalculator y VentaServiceImpl.demandaStockPropio.
        // Con receta o kit, el service los persiste igual pero quedan
        // inertes: el disponible/costo real sale del cálculo correspondiente.
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        @DecimalMin(value = "0.0", message = "El costo unitario no puede ser negativo")
        BigDecimal costoUnitario
) {}
