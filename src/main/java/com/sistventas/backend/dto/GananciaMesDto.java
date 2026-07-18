package com.sistventas.backend.dto;

import java.math.BigDecimal;

// mes en formato ISO "yyyy-MM" (ej. "2026-07") — el frontend arma el label.
public record GananciaMesDto(String mes, BigDecimal ganancia) {}
