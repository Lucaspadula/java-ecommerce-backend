package com.sistventas.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// Mismo shape de item que PublicPedidoRequest.items (productoId + cantidad):
// el precio y la categoría NUNCA viajan desde el cliente, siempre se
// resuelven server-side contra el producto persistido (ver
// PublicTiendaServiceImpl.previewDescuentoCombo).
public record PreviewDescuentoComboRequest(
        @NotEmpty(message = "El carrito debe tener al menos un item")
        @Valid
        List<PublicPedidoItemRequest> items
) {}
