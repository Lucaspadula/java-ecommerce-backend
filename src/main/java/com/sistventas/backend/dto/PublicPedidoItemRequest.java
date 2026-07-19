package com.sistventas.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PublicPedidoItemRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad,

        // null para productos sin variantes (comportamiento de siempre). Si
        // el producto SÍ tiene variantes cargadas, es obligatorio elegir una
        // — se valida en PublicTiendaServiceImpl.crearPedido, no acá, porque
        // depende de datos del producto persistido.
        Long varianteId
) {}
