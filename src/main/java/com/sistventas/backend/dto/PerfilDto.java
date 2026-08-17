package com.sistventas.backend.dto;

import com.sistventas.backend.entity.RolEmpresa;

import java.math.BigDecimal;

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
        // Nunca se devuelve la key en texto plano, solo si ya hay una
        // configurada — ver Empresa.geminiApiKey / PerfilServiceImpl.toDto.
        boolean geminiApiKeyConfigurada
) {}
