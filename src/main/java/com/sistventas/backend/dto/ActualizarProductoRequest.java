package com.sistventas.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

// Casi igual que ProductoRequest — sigue separado como record propio porque
// esta pantalla nunca necesitó diferir en nada más (no por el viejo motivo
// de "sin stock": un producto sin receta SÍ necesita poder editar su stock
// propio acá, ver `stock` más abajo).
public record ActualizarProductoRequest(
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

        // Ya no es @NotEmpty acá — ver ProductoRequest.
        @Valid
        List<ProductoInsumoRequest> insumos,

        // Opcional: no todo producto tiene variantes de color — ver
        // ProductoRequest.
        @Valid
        List<ProductoVarianteRequest> variantes,

        // Kit (Composite) — ver ProductoRequest.
        @Valid
        List<ProductoComponenteRequest> componentes,

        // Lugares grabables — ver ProductoRequest.
        @Valid
        List<ProductoGrabadoRequest> grabados,

        // Producto simple con stock propio — ver ProductoRequest.
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        @DecimalMin(value = "0.0", message = "El costo unitario no puede ser negativo")
        BigDecimal costoUnitario,

        // Atributos de filtro asignados — ver ProductoRequest.
        List<Long> atributoValorIds
) {}
