package com.sistventas.backend.service.impl;

import com.sistventas.backend.entity.ProductoFoto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// JUnit puro (sin contexto de Spring): la regla de negocio no depende de
// nada más que la lista de fotos ya cargada — ver spec "Regla de miniatura
// de catálogo".
class ProductoFotoResolverTest {

    private final ProductoFotoResolver resolver = new ProductoFotoResolver();

    @Test
    void conAlMenosUnaFotoSinColorDevuelveLaPrimeraSinColorPorOrden() {
        ProductoFoto conColor = foto(1L, 5L, "/rojo.jpg", 0);
        ProductoFoto sinColor = foto(2L, null, "/general.jpg", 1);
        ProductoFoto otraConColor = foto(3L, 6L, "/azul.jpg", 2);

        String miniatura = resolver.resolverMiniatura(List.of(conColor, sinColor, otraConColor));

        assertThat(miniatura).isEqualTo("/general.jpg");
    }

    @Test
    void conTodasLasFotosConColorDevuelveLaPrimeraDelPoolPorOrden() {
        ProductoFoto rojo = foto(1L, 5L, "/rojo.jpg", 0);
        ProductoFoto azul = foto(2L, 6L, "/azul.jpg", 1);

        String miniatura = resolver.resolverMiniatura(List.of(azul, rojo));

        // azul tiene orden 1, rojo orden 0: el orden manda, no el orden de la lista de entrada.
        assertThat(miniatura).isEqualTo("/rojo.jpg");
    }

    @Test
    void conListaVaciaDevuelveNull() {
        assertThat(resolver.resolverMiniatura(List.of())).isNull();
    }

    @Test
    void fotosDeVarianteFiltraSoloLasDeEseVarianteIdOrdenadasPorOrden() {
        ProductoFoto general = foto(1L, null, "/general.jpg", 0);
        ProductoFoto rojoB = foto(2L, 5L, "/rojo-b.jpg", 2);
        ProductoFoto rojoA = foto(3L, 5L, "/rojo-a.jpg", 1);
        ProductoFoto azul = foto(4L, 6L, "/azul.jpg", 3);

        List<ProductoFoto> resultado = resolver.fotosDeVariante(List.of(general, rojoB, rojoA, azul), 5L);

        assertThat(resultado).extracting(ProductoFoto::getUrl).containsExactly("/rojo-a.jpg", "/rojo-b.jpg");
    }

    @Test
    void fotosDeVarianteSinCoincidenciasDevuelveListaVacia() {
        ProductoFoto general = foto(1L, null, "/general.jpg", 0);

        List<ProductoFoto> resultado = resolver.fotosDeVariante(List.of(general), 999L);

        assertThat(resultado).isEmpty();
    }

    private ProductoFoto foto(Long id, Long varianteId, String url, int orden) {
        ProductoFoto foto = new ProductoFoto();
        foto.setId(id);
        foto.setVarianteId(varianteId);
        foto.setUrl(url);
        foto.setOrden(orden);
        return foto;
    }
}
