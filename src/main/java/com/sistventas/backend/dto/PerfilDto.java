package com.sistventas.backend.dto;

import com.sistventas.backend.entity.RolEmpresa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PerfilDto(
        Long id,
        String nombre,
        String email,
        RolEmpresa rolEmpresa,
        String empresaNombre,
        boolean esSuperAdmin,
        String logoUrl,
        String empresaSlug,
        boolean tiendaHabilitada,
        String tiendaBannerTitulo,
        String tiendaBannerDescripcion,
        String tiendaBannerTagline,
        String tiendaContactoWhatsapp,
        String tiendaContactoInstagram,
        String tiendaContactoEmail,
        String tiendaCuponCodigo,
        BigDecimal tiendaCuponPorcentaje,
        String tiendaFuente,
        String tiendaTema,
        String tiendaBannerVerticalPosicion,
        String tiendaRazonSocial,
        String tiendaCuit,
        String tiendaDireccion,
        String tiendaSobreNosotros,
        boolean tiendaOfertaActiva,
        String tiendaOfertaEtiqueta,
        String tiendaOfertaTexto,
        LocalDateTime tiendaOfertaFechaFin,
        // Nunca se devuelve la key en texto plano, solo si ya hay una
        // configurada — ver Empresa.geminiApiKey / PerfilServiceImpl.toDto.
        boolean geminiApiKeyConfigurada,
        // null = portada del catálogo PDF con el color oscuro fijo de
        // siempre (ver CatalogoServiceImpl.dibujarPortada).
        String catalogoPortadaImagenUrl,
        boolean catalogoMostrarLogo,
        // null = la portada usa el nombre de la empresa (comportamiento de
        // siempre) — ver CatalogoServiceImpl.dibujarPortada.
        String catalogoTituloPersonalizado,
        boolean catalogoMostrarDescripcion,
        boolean catalogoMostrarColores,
        // null = fondo blanco de siempre en las páginas de productos del PDF.
        String catalogoColorFondoProductos,
        // Los 3 siguientes son el estilo GLOBAL del texto de "Importante"/
        // "Cómo comprar" — null = default (Helvetica, 10pt, gris oscuro).
        String catalogoTextoFuente,
        Integer catalogoTextoTamanio,
        String catalogoTextoColor
) {}
