package com.sistventas.backend.service.impl;

import com.sistventas.backend.entity.ProductoFoto;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

// Fuente única de verdad para "cuál es la miniatura de catálogo de un
// producto" y "qué fotos tiene una variante puntual" — mismo criterio que
// StockDisponibleCalculator: un componente compartido por ProductoServiceImpl,
// PublicTiendaServiceImpl y CatalogoServiceImpl (PDF/Excel) para que los tres
// vean siempre el mismo resultado sin duplicar la regla (ver spec "Regla de
// miniatura de catálogo").
@Component
public class ProductoFotoResolver {

    // Regla: la primera foto SIN color (por orden); si todas tienen color, la
    // primera del pool por orden. null si no hay ninguna foto cargada.
    public String resolverMiniatura(List<ProductoFoto> fotos) {
        return fotos.stream()
                .filter(foto -> foto.getVarianteId() == null)
                .min(Comparator.comparingInt(ProductoFoto::getOrden))
                .or(() -> fotos.stream().min(Comparator.comparingInt(ProductoFoto::getOrden)))
                .map(ProductoFoto::getUrl)
                .orElse(null);
    }

    // Subconjunto del pool etiquetado con ESE variante_id, ordenado por
    // orden — usado para resolver la foto propia de una variante de color
    // (ver ProductoVarianteDto.fotoUrl / PublicVarianteDto.fotoUrl, ambos
    // derivados de acá, primera de este subconjunto).
    public List<ProductoFoto> fotosDeVariante(List<ProductoFoto> fotos, Long varianteId) {
        return fotos.stream()
                .filter(foto -> varianteId.equals(foto.getVarianteId()))
                .sorted(Comparator.comparingInt(ProductoFoto::getOrden))
                .toList();
    }
}
