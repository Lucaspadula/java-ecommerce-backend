package com.sistventas.backend.dto;

import java.math.BigDecimal;

// Lugar grabable de un producto, con su precio. A diferencia de
// ProductoVarianteDto, acá SÍ se expone el id: la tienda pública lo manda de
// vuelta en PublicPedidoItemRequest para indicar qué lugar(es) eligió el
// cliente (ver PublicTiendaServiceImpl.crearPedido).
public record ProductoGrabadoDto(
        Long id,
        String lugar,
        BigDecimal precio
) {}
