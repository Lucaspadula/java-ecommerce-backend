package com.sistventas.backend.service.impl;

import com.sistventas.backend.entity.ReglaDescuentoCombo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Tests del motor de descuento por combo: es la pieza que calcula plata real
// en cada pedido público, así que cada regla de negocio confirmada con el
// dueño (pares completos, mejor regla gana, menor precio primero, % solo
// sobre los pares) tiene su caso acá. El motor es puro (sin base de datos) a
// propósito para que estos tests no necesiten Spring ni fixtures pesados.
class CalculadorDescuentoComboServiceTest {

    private static final Long CAT_MATES = 1L;
    private static final Long CAT_BOMBILLAS = 2L;
    private static final Long CAT_TERMOS = 3L;
    private static final Long SUB_ALGARROBO = 10L;

    private final CalculadorDescuentoComboService calculador = new CalculadorDescuentoComboService();

    @Test
    void sinReglasActivasNoHayDescuento() {
        List<LineaCarritoCombo> lineas = List.of(
                linea(1L, 1, "1000", CAT_MATES, null));

        ResultadoDescuentoCombo resultado = calculador.calcular(lineas, List.of());

        assertThat(resultado.montoDescuento()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.reglaAplicada()).isNull();
    }

    @Test
    void parSimpleDescuentaElPorcentajeSobreAmbosPrecios() {
        // 1 mate ($1000) + 1 bombilla ($500), regla 10% -> 10% de $1500 = $150
        List<LineaCarritoCombo> lineas = List.of(
                linea(1L, 1, "1000", CAT_MATES, null),
                linea(2L, 1, "500", CAT_BOMBILLAS, null));

        ResultadoDescuentoCombo resultado = calculador.calcular(lineas, List.of(regla(CAT_MATES, CAT_BOMBILLAS, "10")));

        assertThat(resultado.pares()).isEqualTo(1);
        assertThat(resultado.montoDescuento()).isEqualByComparingTo("150.00");
        assertThat(resultado.detalle()).isEqualTo("Mates + Bombillas");
    }

    @Test
    void soloLosParesCompletosDescuentan() {
        // 2 mates + 1 bombilla -> un solo par completo: el segundo mate queda
        // sin pareja y no recibe descuento.
        List<LineaCarritoCombo> lineas = List.of(
                linea(1L, 2, "1000", CAT_MATES, null),
                linea(2L, 1, "500", CAT_BOMBILLAS, null));

        ResultadoDescuentoCombo resultado = calculador.calcular(lineas, List.of(regla(CAT_MATES, CAT_BOMBILLAS, "10")));

        assertThat(resultado.pares()).isEqualTo(1);
        assertThat(resultado.montoDescuento()).isEqualByComparingTo("150.00");
    }

    @Test
    void cadaUnidadDeUnaLineaPuedeFormarSuPropioPar() {
        // 2 mates + 2 bombillas -> 2 pares completos, el descuento se aplica
        // una vez por cada par (no una vez por carrito).
        List<LineaCarritoCombo> lineas = List.of(
                linea(1L, 2, "1000", CAT_MATES, null),
                linea(2L, 2, "500", CAT_BOMBILLAS, null));

        ResultadoDescuentoCombo resultado = calculador.calcular(lineas, List.of(regla(CAT_MATES, CAT_BOMBILLAS, "10")));

        assertThat(resultado.pares()).isEqualTo(2);
        assertThat(resultado.montoDescuento()).isEqualByComparingTo("300.00");
    }

    @Test
    void conPreciosDistintosElParSeArmaConElMasBarato() {
        // Dos mates de precio distinto y una sola bombilla: el par usa el
        // mate MAS BARATO (decisión conservadora para el margen, confirmada
        // con el dueño) — descuento = 10% de (800 + 500), no de (2000 + 500).
        List<LineaCarritoCombo> lineas = List.of(
                linea(1L, 1, "2000", CAT_MATES, null),
                linea(2L, 1, "800", CAT_MATES, null),
                linea(3L, 1, "500", CAT_BOMBILLAS, null));

        ResultadoDescuentoCombo resultado = calculador.calcular(lineas, List.of(regla(CAT_MATES, CAT_BOMBILLAS, "10")));

        assertThat(resultado.montoDescuento()).isEqualByComparingTo("130.00");
    }

    @Test
    void entreVariasReglasGanaLaDeMayorMontoTotalNoLaDeMayorPorcentaje() {
        // Regla A: Mates+Bombillas al 20% sobre ($100+$100) = $40.
        // Regla B: Mates+Termos al 10% sobre ($100+$5000) = $510.
        // Gana B aunque su % sea menor — se compara el monto final en pesos.
        List<LineaCarritoCombo> lineas = List.of(
                linea(1L, 1, "100", CAT_MATES, null),
                linea(2L, 1, "100", CAT_BOMBILLAS, null),
                linea(3L, 1, "5000", CAT_TERMOS, null));

        ResultadoDescuentoCombo resultado = calculador.calcular(lineas, List.of(
                regla(CAT_MATES, CAT_BOMBILLAS, "20"),
                regla(CAT_MATES, CAT_TERMOS, "10")));

        assertThat(resultado.montoDescuento()).isEqualByComparingTo("510.00");
        assertThat(resultado.detalle()).isEqualTo("Mates + Termos");
    }

    @Test
    void lasReglasNuncaSeSumanEntreSi() {
        // El carrito califica para dos reglas a la vez pero el descuento
        // final es el de UNA sola (la mejor), no la suma de ambas.
        List<LineaCarritoCombo> lineas = List.of(
                linea(1L, 1, "1000", CAT_MATES, null),
                linea(2L, 1, "1000", CAT_BOMBILLAS, null),
                linea(3L, 1, "1000", CAT_TERMOS, null));

        ResultadoDescuentoCombo resultado = calculador.calcular(lineas, List.of(
                regla(CAT_MATES, CAT_BOMBILLAS, "10"),
                regla(CAT_MATES, CAT_TERMOS, "10")));

        // Cada regla por separado daría $200; sumadas serían $400.
        assertThat(resultado.montoDescuento()).isEqualByComparingTo("200.00");
    }

    @Test
    void reglaSinSubcategoriaMatcheaCualquierSubcategoriaDeLaCategoria() {
        List<LineaCarritoCombo> lineas = List.of(
                linea(1L, 1, "1000", CAT_MATES, SUB_ALGARROBO),
                linea(2L, 1, "500", CAT_BOMBILLAS, null));

        ResultadoDescuentoCombo resultado = calculador.calcular(lineas, List.of(regla(CAT_MATES, CAT_BOMBILLAS, "10")));

        assertThat(resultado.pares()).isEqualTo(1);
    }

    @Test
    void reglaConSubcategoriaSoloMatcheaEsaSubcategoria() {
        // La regla exige Mates/Algarrobo del lado A; el mate del carrito no
        // tiene esa subcategoría -> ningún par.
        ReglaDescuentoCombo conSubcategoria = regla(CAT_MATES, CAT_BOMBILLAS, "10");
        conSubcategoria.setSubcategoriaAId(SUB_ALGARROBO);

        List<LineaCarritoCombo> lineas = List.of(
                linea(1L, 1, "1000", CAT_MATES, null),
                linea(2L, 1, "500", CAT_BOMBILLAS, null));

        ResultadoDescuentoCombo resultado = calculador.calcular(lineas, List.of(conSubcategoria));

        assertThat(resultado.montoDescuento()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void elMontoSeRedondeaADosDecimales() {
        // 10% de ($33.33 + $33.34) = $6.667 -> $6.67 (HALF_UP)
        List<LineaCarritoCombo> lineas = List.of(
                linea(1L, 1, "33.33", CAT_MATES, null),
                linea(2L, 1, "33.34", CAT_BOMBILLAS, null));

        ResultadoDescuentoCombo resultado = calculador.calcular(lineas, List.of(regla(CAT_MATES, CAT_BOMBILLAS, "10")));

        assertThat(resultado.montoDescuento()).isEqualByComparingTo("6.67");
    }

    private LineaCarritoCombo linea(Long productoId, int cantidad, String precio, Long categoriaId, Long subcategoriaId) {
        String nombre = nombreCategoria(categoriaId);
        return new LineaCarritoCombo(productoId, cantidad, new BigDecimal(precio), categoriaId, nombre, subcategoriaId, null);
    }

    private String nombreCategoria(Long categoriaId) {
        if (CAT_MATES.equals(categoriaId)) return "Mates";
        if (CAT_BOMBILLAS.equals(categoriaId)) return "Bombillas";
        return "Termos";
    }

    private ReglaDescuentoCombo regla(Long categoriaAId, Long categoriaBId, String porcentaje) {
        ReglaDescuentoCombo regla = new ReglaDescuentoCombo();
        regla.setEmpresaId(1L);
        regla.setCategoriaAId(categoriaAId);
        regla.setCategoriaBId(categoriaBId);
        regla.setPorcentaje(new BigDecimal(porcentaje));
        regla.setActivo(true);
        return regla;
    }
}
