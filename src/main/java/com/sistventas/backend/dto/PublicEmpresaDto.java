package com.sistventas.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PublicEmpresaDto(
        String nombre,
        String logoUrl,
        String bannerTitulo,
        String bannerDescripcion,
        String bannerTagline,
        List<BannerImagenPublicaDto> bannerImagenes,
        String contactoWhatsapp,
        String contactoInstagram,
        String contactoEmail,
        String tiendaFuente,
        String tiendaTema,
        // Sección nueva, separada del hero (bannerImagenes de arriba): hasta
        // 12 imágenes verticales, cada una con producto opcional. Se muestra
        // en la tienda pública solo si esta lista no está vacía Y
        // bannerVerticalPosicion no es null (ver Empresa.tiendaBannerVerticalPosicion).
        List<BannerImagenPublicaDto> bannerVerticales,
        String bannerVerticalPosicion,
        // Los 4 siguientes son opcionales, para más presencia legal en el
        // footer ("Sobre nosotros" es texto libre, el resto son datos
        // formales de la empresa). null = no cargado, el frontend omite esa
        // parte sin dejar separadores sueltos (ver TiendaPublica.footerLegal).
        String razonSocial,
        String cuit,
        String direccion,
        String sobreNosotros,
        // null = sin oferta activa; el frontend no debe mostrar la barra de
        // urgencia en ese caso. Cuando no es null, ofertaFechaFin siempre es
        // una fecha futura — PublicTiendaServiceImpl ya filtró las vencidas
        // (ver Empresa.tiendaOfertaActiva/tiendaOfertaFechaFin).
        String ofertaEtiqueta,
        String ofertaTexto,
        LocalDateTime ofertaFechaFin
) {}
