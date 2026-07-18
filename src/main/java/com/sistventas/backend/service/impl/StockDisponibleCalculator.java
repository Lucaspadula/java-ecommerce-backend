package com.sistventas.backend.service.impl;

import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ProductoComponente;
import com.sistventas.backend.entity.ProductoInsumo;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.util.List;

// Fuente única de verdad para "cuánto stock disponible tiene un producto".
// Se inyecta en cualquier service que necesite ese número (ProductoServiceImpl
// para el DTO de gestión interna, PublicTiendaServiceImpl para el catálogo y
// los pedidos públicos), así todos ven siempre el mismo criterio sin
// duplicar el cálculo.
//
// - Producto SIN receta ni componentes: el stock es el campo propio del
//   producto, cargado al alta y movido por ventas/ajustes manuales.
// - Producto CON receta: Lucas no tiene unidades armadas guardadas, arma
//   cada producto en el momento a partir de insumos sueltos. El disponible
//   es lo que alcanza a armar: para cada línea de la receta se calcula
//   insumo.stock / cantidadNecesaria (división entera hacia abajo) y el
//   resultado final es el MÍNIMO entre todas las líneas — el insumo que
//   menos alcanza limita cuántas unidades se pueden armar.
// - Producto KIT (con componentes): mismo espíritu, un nivel más arriba — el
//   disponible es el mínimo entre lo que alcanza de cada componente
//   (disponible del componente / cantidad que el kit necesita de él) y, si
//   el kit tiene receta directa propia (su embalaje compartido), también el
//   mínimo de esa receta. Los componentes se evalúan con la receta EN COMBO
//   (sin embalaje, ver ConsumoEnComboStrategy). Límite conocido de esta
//   fase: un componente que a su vez sea kit se evalúa como producto simple
//   (no se recorre su propia composición) — kits de un solo nivel.
@Component
public class StockDisponibleCalculator {

    private final ConsumoEnComboStrategy consumoEnCombo;

    public StockDisponibleCalculator(ConsumoEnComboStrategy consumoEnCombo) {
        this.consumoEnCombo = consumoEnCombo;
    }

    public int calcular(Producto producto) {
        if (!producto.getComponentes().isEmpty()) {
            return calcularKit(producto);
        }
        if (producto.getInsumos().isEmpty()) {
            return producto.getStock();
        }
        return minimoPorReceta(producto.getInsumos());
    }

    private int calcularKit(Producto kit) {
        int disponible = Integer.MAX_VALUE;
        for (ProductoComponente componente : kit.getComponentes()) {
            int delComponente = disponibleComoComponente(componente.getComponenteProducto());
            int cantidadNecesaria = Math.max(1, componente.getCantidad());
            disponible = Math.min(disponible, delComponente / cantidadNecesaria);
        }
        // La receta directa del kit (embalaje compartido) también limita:
        // sin bolsas de regalo no se arma el combo aunque sobren mates.
        if (!kit.getInsumos().isEmpty()) {
            disponible = Math.min(disponible, minimoPorReceta(kit.getInsumos()));
        }
        return disponible == Integer.MAX_VALUE ? 0 : disponible;
    }

    // Un componente dentro de un kit no consume su embalaje propio (lo pone
    // el kit), así que su disponibilidad se evalúa solo sobre la materia
    // prima real. Si su receta quedara vacía tras el filtro (receta compuesta
    // SOLO de embalaje), dentro del combo no consume nada y no limita.
    private int disponibleComoComponente(Producto componente) {
        if (componente.getInsumos().isEmpty()) {
            return componente.getStock();
        }
        List<ProductoInsumo> lineas = consumoEnCombo.resolver(componente);
        if (lineas.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return minimoPorReceta(lineas);
    }

    private int minimoPorReceta(List<ProductoInsumo> lineas) {
        return lineas.stream()
                .mapToInt(productoInsumo -> productoInsumo.getInsumo().getStock()
                        .divide(productoInsumo.getCantidad(), RoundingMode.DOWN)
                        .intValue())
                .min()
                .orElse(0);
    }
}
