package com.sistventas.backend.dto;

// Línea de un kit: otro Producto ya cargado + cuántas unidades de él entran
// en el combo. componenteNombre se resuelve acá (igual que insumoNombre en
// ProductoInsumoDto) para que el frontend no tenga que ir a buscarlo aparte.
public record ProductoComponenteDto(
        Long id,
        Long componenteProductoId,
        String componenteNombre,
        Integer cantidad
) {}
