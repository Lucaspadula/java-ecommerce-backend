package com.sistventas.backend.dto;

import com.sistventas.backend.entity.EstadoPedido;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ProveedorRequest(
        @NotBlank(message = "El nombre del proveedor es obligatorio")
        String nombre,

        String contacto,

        String notas,

        EstadoPedido estadoPedido,

        String detallePedidoActual,

        LocalDate fechaPedido,

        LocalDate fechaLlegadaEstimada,

        String ultimoPedidoDetalle
) {}
