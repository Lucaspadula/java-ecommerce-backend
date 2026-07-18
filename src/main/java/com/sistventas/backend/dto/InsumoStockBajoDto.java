package com.sistventas.backend.dto;

import java.math.BigDecimal;

public record InsumoStockBajoDto(Long id, String nombre, BigDecimal stock, BigDecimal stockMinimo) {}
