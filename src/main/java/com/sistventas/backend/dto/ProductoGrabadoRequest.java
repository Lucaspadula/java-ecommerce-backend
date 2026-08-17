package com.sistventas.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Línea de lugar grabable enviada al crear/editar un producto. Sin id, a
// diferencia de ProductoVarianteRequest: nada externo referencia una fila de
// ProductoGrabado por id de forma persistente (el pedido público solo la usa
// al momento de armar el precio/texto de la venta, ver
// PublicTiendaServiceImpl.crearPedido), así que ProductoServiceImpl puede
// hacer reemplazo completo en cada edición — mismo patrón que
// ProductoComponenteRequest.
public record ProductoGrabadoRequest(
        @NotBlank(message = "El lugar es obligatorio")
        String lugar,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
        BigDecimal precio
) {}
