package com.sistventas.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Reseña dejada por el cliente desde la vidriera pública, sin login: la
// "autenticación" es el mismo par (ventaId + telefono) que ya usa
// "Seguimiento de pedido" (ver PublicTiendaService.consultarPedido) — el
// backend valida que esa venta exista, esté ENTREGADA, tenga este producto
// entre sus items, y que el teléfono coincida con el cliente de la venta.
public record CrearResenaClienteRequest(
        @NotNull(message = "El número de pedido es obligatorio")
        Long ventaId,

        @NotBlank(message = "El teléfono es obligatorio")
        String telefono,

        @NotNull(message = "La puntuación es obligatoria")
        @Min(value = 1, message = "La puntuación mínima es 1")
        @Max(value = 5, message = "La puntuación máxima es 5")
        Integer puntuacion,

        // Opcional: el cliente puede dejar solo las estrellas.
        @Size(max = 1000, message = "El comentario no puede superar los 1000 caracteres")
        String comentario
) {}
