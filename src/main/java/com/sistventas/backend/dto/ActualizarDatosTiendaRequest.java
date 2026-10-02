package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// PUT /api/empresa/tienda/datos: dirección (slug), habilitación, contacto,
// datos legales y "Sobre nosotros".
public record ActualizarDatosTiendaRequest(
        @NotBlank(message = "El identificador de tienda es obligatorio")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "El slug solo puede tener minúsculas, números y guiones")
        String slug,

        @NotNull(message = "El estado de la tienda es obligatorio")
        Boolean tiendaHabilitada,

        @Size(max = 40, message = "El WhatsApp no puede superar los 40 caracteres")
        String tiendaContactoWhatsapp,

        @Size(max = 100, message = "El Instagram no puede superar los 100 caracteres")
        String tiendaContactoInstagram,

        @Size(max = 190, message = "El email no puede superar los 190 caracteres")
        String tiendaContactoEmail,

        @Size(max = 150, message = "La razón social no puede superar los 150 caracteres")
        String tiendaRazonSocial,

        @Size(max = 20, message = "El CUIT no puede superar los 20 caracteres")
        String tiendaCuit,

        @Size(max = 200, message = "La dirección no puede superar los 200 caracteres")
        String tiendaDireccion,

        @Size(max = 2000, message = "El texto no puede superar los 2000 caracteres")
        String tiendaSobreNosotros
) {}
