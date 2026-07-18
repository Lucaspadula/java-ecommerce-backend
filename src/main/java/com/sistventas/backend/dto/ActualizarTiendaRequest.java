package com.sistventas.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ActualizarTiendaRequest(
        @NotBlank(message = "El identificador de tienda es obligatorio")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "El slug solo puede tener minúsculas, números y guiones")
        String slug,

        @NotNull(message = "El estado de la tienda es obligatorio")
        Boolean tiendaHabilitada,

        // Ambos opcionales: si vienen vacíos/null, la vidriera pública usa un
        // texto genérico en vez del personalizado.
        @Size(max = 150, message = "El título no puede superar los 150 caracteres")
        String tiendaBannerTitulo,

        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String tiendaBannerDescripcion,

        @Size(max = 100, message = "El subtítulo no puede superar los 100 caracteres")
        String tiendaBannerTagline,

        // Todos opcionales: contacto y cupón son personalización adicional,
        // no obligatoria para tener la tienda habilitada.
        @Size(max = 40, message = "El WhatsApp no puede superar los 40 caracteres")
        String tiendaContactoWhatsapp,

        @Size(max = 100, message = "El Instagram no puede superar los 100 caracteres")
        String tiendaContactoInstagram,

        @Size(max = 190, message = "El email no puede superar los 190 caracteres")
        String tiendaContactoEmail,

        @Size(max = 40, message = "El código de cupón no puede superar los 40 caracteres")
        String tiendaCuponCodigo,

        @DecimalMin(value = "0.0", message = "El porcentaje del cupón no puede ser negativo")
        @DecimalMax(value = "100.0", message = "El porcentaje del cupón no puede superar 100")
        BigDecimal tiendaCuponPorcentaje,

        // Opcional: si viene null/vacío, se guarda "clasica" (ver
        // PerfilServiceImpl.actualizarTienda). Valores válidos: "clasica",
        // "cursiva", "elegante", "moderna".
        @Size(max = 20, message = "La tipografía no puede superar los 20 caracteres")
        String tiendaFuente,

        // Opcional: null = sección de banners verticales sin ubicación
        // configurada, no se muestra en la tienda pública aunque haya
        // imágenes cargadas. La lista de 4 valores válidos la controla el
        // <select> del frontend, no hace falta duplicarla acá.
        @Size(max = 30, message = "La ubicación no puede superar los 30 caracteres")
        String tiendaBannerVerticalPosicion,

        // Los 3 siguientes son opcionales, para más presencia legal en el
        // footer de la tienda pública.
        @Size(max = 150, message = "La razón social no puede superar los 150 caracteres")
        String tiendaRazonSocial,

        @Size(max = 20, message = "El CUIT no puede superar los 20 caracteres")
        String tiendaCuit,

        @Size(max = 200, message = "La dirección no puede superar los 200 caracteres")
        String tiendaDireccion,

        // Opcional: texto libre largo mostrado en el modal "Sobre nosotros"
        // de la tienda pública.
        @Size(max = 2000, message = "El texto no puede superar los 2000 caracteres")
        String tiendaSobreNosotros
) {}
