package com.sistventas.backend.dto;

import com.sistventas.backend.entity.EstadoVenta;

import java.time.LocalDateTime;

// Sin el usuario que hizo el cambio: eso es info interna, no del cliente
// final que consulta el estado de su pedido.
public record PublicPedidoHistorialDto(
        EstadoVenta estadoAnterior,
        EstadoVenta estadoNuevo,
        LocalDateTime fecha
) {}
