package com.sistventas.backend.dto;

// mes en formato ISO "yyyy-MM" (ej. "2026-07") — el frontend arma el label.
public record UnidadesMesDto(String mes, int unidades) {}
