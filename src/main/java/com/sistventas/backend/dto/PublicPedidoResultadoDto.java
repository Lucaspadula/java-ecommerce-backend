package com.sistventas.backend.dto;

import java.math.BigDecimal;

public record PublicPedidoResultadoDto(
        Long ventaId,
        BigDecimal total
) {}
