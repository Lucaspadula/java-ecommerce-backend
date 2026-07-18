package com.sistventas.backend.dto;

// productoId null = desvincular el producto ya asociado a la imagen (dejarla
// sin click especial). No lleva @NotNull a propósito: null es un valor
// válido y esperado acá, no un dato faltante.
public record ActualizarProductoBannerImagenRequest(
        Long productoId
) {}
