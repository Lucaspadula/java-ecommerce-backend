package com.sistventas.backend.service.impl;

import com.sistventas.backend.entity.ReglaDescuentoCombo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Motor de cálculo del descuento automático por combo de categorías. Reglas
 * de negocio (todas confirmadas con el usuario, ver conversación de diseño):
 *
 * 1) "Pares completos": si el carrito tiene 2 productos del lado A y 1 del
 *    lado B, se forma 1 par (el mínimo entre las cantidades de cada lado). El
 *    % se aplica una vez POR CADA par completo.
 * 2) "Mejor regla gana": si el carrito califica para más de una regla activa
 *    a la vez, se aplica solo la que da el MAYOR descuento total en $ — nunca
 *    se suman entre sí.
 * 3) El % se calcula solo sobre el precio de los productos que forman cada
 *    par específico (un producto de cada lado por par), no sobre el total
 *    del carrito.
 * 4) Cuando un lado matchea varios productos distintos con precios distintos
 *    (ej. dos modelos de Mate), los pares se arman de MENOR precio primero
 *    (greedy ascendente) — decisión conservadora para el margen, confirmada
 *    explícitamente con el usuario en vez de maximizar el descuento.
 *
 * Puro y sin estado: no toca la base, así que es fácil de testear con casos
 * simples (ver ReglaDescuentoComboServiceImpl/PublicTiendaServiceImpl para
 * quién lo llama).
 */
@Component
public class CalculadorDescuentoComboService {

    public ResultadoDescuentoCombo calcular(List<LineaCarritoCombo> lineas, List<ReglaDescuentoCombo> reglasActivas) {
        ResultadoDescuentoCombo mejor = ResultadoDescuentoCombo.sinDescuento();
        for (ReglaDescuentoCombo regla : reglasActivas) {
            ResultadoDescuentoCombo resultado = evaluarRegla(lineas, regla);
            // Estrictamente mayor: ante empate, gana la primera regla
            // evaluada (orden estable, comportamiento predecible).
            if (resultado.montoDescuento().compareTo(mejor.montoDescuento()) > 0) {
                mejor = resultado;
            }
        }
        return mejor;
    }

    private ResultadoDescuentoCombo evaluarRegla(List<LineaCarritoCombo> lineas, ReglaDescuentoCombo regla) {
        List<LineaCarritoCombo> lineasLadoA = lineasQueMatchean(lineas, regla.getCategoriaAId(), regla.getSubcategoriaAId());
        List<LineaCarritoCombo> lineasLadoB = lineasQueMatchean(lineas, regla.getCategoriaBId(), regla.getSubcategoriaBId());

        List<BigDecimal> preciosLadoA = expandirPrecios(lineasLadoA);
        List<BigDecimal> preciosLadoB = expandirPrecios(lineasLadoB);

        int pares = Math.min(preciosLadoA.size(), preciosLadoB.size());
        if (pares == 0) {
            return ResultadoDescuentoCombo.sinDescuento();
        }

        // Menor precio primero de cada lado (ver punto 4 arriba).
        preciosLadoA.sort(Comparator.naturalOrder());
        preciosLadoB.sort(Comparator.naturalOrder());

        BigDecimal montoBase = BigDecimal.ZERO;
        for (int i = 0; i < pares; i++) {
            montoBase = montoBase.add(preciosLadoA.get(i)).add(preciosLadoB.get(i));
        }

        BigDecimal montoDescuento = montoBase
                .multiply(regla.getPorcentaje())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // El matcheo es por id (ver matchea más abajo); el texto legible sale
        // del nombre de categoría de cualquiera de las líneas que matchearon
        // cada lado (todas comparten el mismo categoriaId, así que comparten
        // el mismo nombre) — nunca de la regla, que ya no guarda texto.
        String detalle = lineasLadoA.get(0).categoriaNombre() + " + " + lineasLadoB.get(0).categoriaNombre();
        return new ResultadoDescuentoCombo(regla, pares, montoDescuento, detalle);
    }

    private List<LineaCarritoCombo> lineasQueMatchean(List<LineaCarritoCombo> lineas, Long categoriaId, Long subcategoriaId) {
        return lineas.stream().filter(linea -> matchea(linea, categoriaId, subcategoriaId)).toList();
    }

    // Cada UNIDAD del carrito que matchea cuenta como una entrada de precio
    // individual (no una sola vez por línea): así una línea con cantidad=3 de
    // un producto que matchea puede aportar hasta 3 pares distintos.
    private List<BigDecimal> expandirPrecios(List<LineaCarritoCombo> lineasQueMatchean) {
        List<BigDecimal> precios = new ArrayList<>();
        for (LineaCarritoCombo linea : lineasQueMatchean) {
            for (int i = 0; i < linea.cantidad(); i++) {
                precios.add(linea.precioUnitario());
            }
        }
        return precios;
    }

    // subcategoriaId de la regla en null = matchea cualquier subcategoría de
    // esa categoría (ver ReglaDescuentoCombo.subcategoriaAId/BId). Comparación
    // por id, no por texto: dos categorías con el mismo nombre pero ids
    // distintos (no debería pasar dentro de una empresa, la tabla maestra
    // tiene nombre único por empresa) NUNCA matchean entre sí.
    private boolean matchea(LineaCarritoCombo linea, Long categoriaId, Long subcategoriaId) {
        if (!categoriaId.equals(linea.categoriaId())) {
            return false;
        }
        if (subcategoriaId == null) {
            return true;
        }
        return subcategoriaId.equals(linea.subcategoriaId());
    }
}
