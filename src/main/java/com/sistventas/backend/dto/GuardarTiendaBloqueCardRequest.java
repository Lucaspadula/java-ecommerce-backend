package com.sistventas.backend.dto;

import jakarta.validation.constraints.Size;

// orientacion null => VERTICAL, accion null => NINGUNA. El resto de las
// reglas (imagen /uploads/, URL http(s), referencias de la empresa, MODAL con
// contenido) se validan en el service (400).
public record GuardarTiendaBloqueCardRequest(
        @Size(max = 500, message = "La imagen no puede superar los 500 caracteres")
        String imagenUrl,
        String orientacion,
        @Size(max = 150, message = "El título no puede superar los 150 caracteres")
        String titulo,
        @Size(max = 500, message = "El texto no puede superar los 500 caracteres")
        String texto,
        String accion,
        @Size(max = 500, message = "El valor de la acción no puede superar los 500 caracteres")
        String accionValor
) {}
