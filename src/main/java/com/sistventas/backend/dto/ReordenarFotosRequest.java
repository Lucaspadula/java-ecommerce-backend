package com.sistventas.backend.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// Body de PUT /api/productos/{id}/fotos/orden: la lista completa de ids de
// foto del producto, en el orden final deseado. El service reasigna
// orden 0..n-1 según la posición en esta lista (ver design Decision #1 —
// endpoint dedicado, no embebido en ActualizarProductoRequest).
public record ReordenarFotosRequest(
        @NotEmpty(message = "La lista de fotos no puede estar vacía")
        List<Long> fotoIds
) {}
