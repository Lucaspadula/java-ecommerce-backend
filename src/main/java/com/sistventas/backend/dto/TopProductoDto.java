package com.sistventas.backend.dto;

import java.math.BigDecimal;

public record TopProductoDto(Long id, String nombre, int cantidadVendida, BigDecimal gananciaTotal) {}
