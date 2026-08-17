package com.sistventas.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

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
        Long varianteId,

        // Los 3 siguientes son opcionales y van juntos: lista vacía/null =
        // sin grabado, comportamiento de siempre. Si viene 1+ lugar, cada id
        // tiene que pertenecer a ESE producto (se valida en crearPedido, no
        // acá — depende del producto persistido, igual que varianteId).
        List<Long> grabadoLugarIds,

        @Size(max = 500, message = "El texto del grabado no puede superar los 500 caracteres")
        String grabadoTexto,

        // URL devuelta por POST /api/public/tienda/{slug}/grabado/foto,
        // subida ANTES de armar el pedido — este request nunca recibe el
        // archivo en sí.
        String grabadoImagenUrl
) {}
