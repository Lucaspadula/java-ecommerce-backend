package com.sistventas.backend.service.impl;

import com.sistventas.backend.entity.Insumo;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ProductoComponente;
import com.sistventas.backend.entity.ProductoInsumo;
import com.sistventas.backend.entity.RolInsumo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// Tests del cálculo de stock disponible, incluyendo el camino nuevo de kits
// (Composite + Strategy). El criterio más importante acá: los productos
// simples (sin componentes) tienen que dar EXACTAMENTE lo mismo que antes de
// existir los kits — cualquier cambio ahí rompería stock real en producción.
class StockDisponibleCalculatorTest {

    private final StockDisponibleCalculator calculator =
            new StockDisponibleCalculator(new ConsumoEnComboStrategy());

    // --- Productos simples: comportamiento pre-kits, sin cambios ---

    @Test
    void productoSinRecetaUsaSuStockPropio() {
        Producto producto = producto();
        producto.setStock(7);

        assertThat(calculator.calcular(producto)).isEqualTo(7);
    }

    @Test
    void productoConRecetaLimitaPorElInsumoQueMenosAlcanza() {
        // 10 maderas / 1 por unidad = 10; 3 virolas / 1 por unidad = 3 -> 3
        Producto producto = producto();
        producto.getInsumos().add(lineaReceta(producto, insumo("10", null), "1"));
        producto.getInsumos().add(lineaReceta(producto, insumo("3", null), "1"));

        assertThat(calculator.calcular(producto)).isEqualTo(3);
    }

    // --- Kits ---

    @Test
    void kitLimitaPorElComponenteQueMenosAlcanza() {
        // Mate: stock propio 5. Termo: stock propio 2. Kit = 1 mate + 1 termo
        // -> alcanzan 2 kits (el termo limita).
        Producto mate = producto();
        mate.setStock(5);
        Producto termo = producto();
        termo.setStock(2);

        Producto kit = kit(componente(mate, 1), componente(termo, 1));

        assertThat(calculator.calcular(kit)).isEqualTo(2);
    }

    @Test
    void kitDivideElDisponibleDelComponentePorLaCantidadQueNecesita() {
        // 5 mates disponibles, el kit lleva 2 mates -> alcanzan 2 kits (floor).
        Producto mate = producto();
        mate.setStock(5);

        Producto kit = kit(componente(mate, 2));

        assertThat(calculator.calcular(kit)).isEqualTo(2);
    }

    @Test
    void elComponenteConRecetaNoConsumeSuEmbalajeDentroDelKit() {
        // Receta del mate: 4 maderas (materia prima) y 1 sola bolsa
        // (embalaje). Vendido suelto, la bolsa limitaría a 1 unidad; dentro
        // del kit el embalaje lo pone el kit, así que solo la madera cuenta
        // -> 4 kits posibles.
        Producto mate = producto();
        mate.getInsumos().add(lineaReceta(mate, insumo("4", RolInsumo.MATERIA_PRIMA), "1"));
        mate.getInsumos().add(lineaReceta(mate, insumo("1", RolInsumo.EMBALAJE), "1"));

        Producto kit = kit(componente(mate, 1));

        assertThat(calculator.calcular(kit)).isEqualTo(4);
    }

    @Test
    void elInsumoSinClasificarSeComportaComoMateriaPrimaDentroDelKit() {
        // rol null (sin clasificar todavía) NO se saltea: sigue limitando,
        // igual que materia prima — nada deja de descontarse por defecto.
        Producto mate = producto();
        mate.getInsumos().add(lineaReceta(mate, insumo("2", null), "1"));

        Producto kit = kit(componente(mate, 1));

        assertThat(calculator.calcular(kit)).isEqualTo(2);
    }

    @Test
    void laRecetaPropiaDelKitTambienLimita() {
        // Componentes alcanzan para 10 kits, pero el kit lleva su propia
        // bolsa de regalo y quedan 3 -> 3 kits.
        Producto mate = producto();
        mate.setStock(10);

        Producto kit = kit(componente(mate, 1));
        kit.getInsumos().add(lineaReceta(kit, insumo("3", RolInsumo.EMBALAJE), "1"));

        assertThat(calculator.calcular(kit)).isEqualTo(3);
    }

    @Test
    void componenteConRecetaSoloDeEmbalajeNoLimitaElKit() {
        // Caso borde: la receta del componente es SOLO embalaje. Dentro del
        // kit no consume nada, así que el límite lo pone el otro componente.
        Producto bolsoDecorativo = producto();
        bolsoDecorativo.getInsumos().add(lineaReceta(bolsoDecorativo, insumo("1", RolInsumo.EMBALAJE), "1"));
        Producto mate = producto();
        mate.setStock(6);

        Producto kit = kit(componente(bolsoDecorativo, 1), componente(mate, 1));

        assertThat(calculator.calcular(kit)).isEqualTo(6);
    }

    // --- Strategies directamente ---

    @Test
    void consumoDirectoDevuelveLaRecetaCompleta() {
        Producto producto = producto();
        producto.getInsumos().add(lineaReceta(producto, insumo("1", RolInsumo.MATERIA_PRIMA), "1"));
        producto.getInsumos().add(lineaReceta(producto, insumo("1", RolInsumo.EMBALAJE), "1"));

        assertThat(new ConsumoDirectoStrategy().resolver(producto)).hasSize(2);
    }

    @Test
    void consumoEnComboExcluyeSoloElEmbalaje() {
        Producto producto = producto();
        producto.getInsumos().add(lineaReceta(producto, insumo("1", RolInsumo.MATERIA_PRIMA), "1"));
        producto.getInsumos().add(lineaReceta(producto, insumo("1", null), "1"));
        producto.getInsumos().add(lineaReceta(producto, insumo("1", RolInsumo.EMBALAJE), "1"));

        assertThat(new ConsumoEnComboStrategy().resolver(producto)).hasSize(2);
    }

    // --- Helpers ---

    private Producto producto() {
        Producto producto = new Producto();
        producto.setStock(0);
        return producto;
    }

    private Producto kit(ProductoComponente... componentes) {
        Producto kit = producto();
        for (ProductoComponente componente : componentes) {
            componente.setProducto(kit);
            kit.getComponentes().add(componente);
        }
        return kit;
    }

    private ProductoComponente componente(Producto componenteProducto, int cantidad) {
        ProductoComponente componente = new ProductoComponente();
        componente.setComponenteProducto(componenteProducto);
        componente.setCantidad(cantidad);
        return componente;
    }

    private Insumo insumo(String stock, RolInsumo rol) {
        Insumo insumo = new Insumo();
        insumo.setStock(new BigDecimal(stock));
        insumo.setRol(rol);
        return insumo;
    }

    private ProductoInsumo lineaReceta(Producto producto, Insumo insumo, String cantidad) {
        ProductoInsumo linea = new ProductoInsumo();
        linea.setProducto(producto);
        linea.setInsumo(insumo);
        linea.setCantidad(new BigDecimal(cantidad));
        return linea;
    }
}
