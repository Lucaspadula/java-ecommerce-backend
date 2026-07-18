package com.sistventas.backend.dto;

// Recorte mínimo de la reseña MÁS RECIENTE de un producto, embebido en
// PublicProductoDto para que el grid de la tienda pública pueda mostrar una
// cita corta sin pedir /resenas por cada producto (ver
// PublicTiendaServiceImpl.listarProductos: se resuelve todo en un solo query
// batch, no un N+1 de requests).
public record ResenaDestacadaDto(
        String clienteNombre,
        String comentario
) {}
