package com.sistventas.backend.service.impl;

import com.sistventas.backend.entity.Producto;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

// Fuente única de verdad para "cuánto stock disponible tiene un producto".
// Se inyecta en cualquier service que necesite ese número (ProductoServiceImpl
// para el DTO de gestión interna, PublicTiendaServiceImpl para el catálogo y
// los pedidos públicos), así todos ven siempre el mismo criterio sin
// duplicar el cálculo.
//
// - Producto SIN receta (insumos vacío): el stock es el campo propio del
//   producto, cargado al alta y movido por ventas/ajustes manuales.
// - Producto CON receta: Lucas no tiene unidades armadas guardadas, arma
//   cada producto en el momento a partir de insumos sueltos. El disponible
//   es lo que alcanza a armar: para cada línea de la receta se calcula
//   insumo.stock / cantidadNecesaria (división entera hacia abajo) y el
//   resultado final es el MÍNIMO entre todas las líneas — el insumo que
//   menos alcanza limita cuántas unidades se pueden armar. Producto.stock
//   deja de leerse como fuente de verdad para estos productos (puede seguir
//   existiendo en la tabla por compatibilidad de esquema, pero nunca se usa
//   este valor).
@Component
public class StockDisponibleCalculator {

    public int calcular(Producto producto) {
        if (producto.getInsumos().isEmpty()) {
            return producto.getStock();
        }
        return producto.getInsumos().stream()
                .mapToInt(productoInsumo -> productoInsumo.getInsumo().getStock()
                        .divide(productoInsumo.getCantidad(), RoundingMode.DOWN)
                        .intValue())
                .min()
                .orElse(0);
    }
}
