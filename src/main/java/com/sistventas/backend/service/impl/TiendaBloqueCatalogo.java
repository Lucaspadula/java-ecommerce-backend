package com.sistventas.backend.service.impl;

import java.util.Set;

/**
 * Valores permitidos y límites de los bloques de la tienda. Los enums se
 * guardan como String en la BD y se validan acá (convención del proyecto).
 */
public final class TiendaBloqueCatalogo {

    public static final Set<String> SLOTS = Set.of(
            "HOME_DESPUES_BANNER", "HOME_DESPUES_CATEGORIAS", "HOME_DESPUES_DESTACADOS", "HOME_ANTES_FOOTER",
            "CATALOGO_SOBRE_GRILLA", "CATALOGO_DEBAJO_GRILLA",
            "PRODUCTO_ANTES_RELACIONADOS", "PRODUCTO_DESPUES_RELACIONADOS");

    public static final Set<String> ANCHOS = Set.of("TERCIO", "MITAD", "DOS_TERCIOS", "COMPLETO");

    public static final Set<String> ORIENTACIONES = Set.of("VERTICAL", "HORIZONTAL");

    public static final Set<String> ACCIONES = Set.of("NINGUNA", "MODAL", "PRODUCTO", "CATEGORIA", "URL");

    public static final int MAX_BLOQUES_POR_EMPRESA = 30;
    public static final int MAX_CARDS_POR_BLOQUE = 12;

    private TiendaBloqueCatalogo() {
    }
}
