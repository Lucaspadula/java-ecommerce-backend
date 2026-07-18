package com.sistventas.backend.dto;

import com.sistventas.backend.entity.EstadoVenta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record VentaDto(
        Long id,
        Long clienteId,
        String clienteNombre,
        EstadoVenta estado,
        LocalDateTime fechaPedido,
        LocalDate fechaEntrega,
        String notas,
        BigDecimal total,
        BigDecimal subtotal,
        BigDecimal descuentoPorcentaje,
        List<VentaItemDto> items,
        LocalDateTime fechaAlta,
        // Opcionales, cargados desde la tienda pública (ver
        // PublicPedidoRequest): null para ventas creadas desde el panel
        // admin, que no pasan por ese formulario.
        String direccionEnvio,
        String localidad,
        String codigoPostal,
        // Descuento automático por combo de categorías, acumulable con
        // descuentoPorcentaje (el cupón) — ver Venta.descuentoComboMonto.
        // Cero/null si no se aplicó ningún combo.
        BigDecimal descuentoComboMonto,
        String descuentoComboDetalle
) {}
