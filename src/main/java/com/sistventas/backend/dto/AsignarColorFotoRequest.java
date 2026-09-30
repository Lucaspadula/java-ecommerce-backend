package com.sistventas.backend.dto;

// Body de PATCH /api/productos/{id}/fotos/{fotoId}: cambia el color asociado
// a una foto sin re-subir el archivo (ver spec "Cambio de color sin
// re-subida"). varianteId null = quita el color (vuelve a "general").
public record AsignarColorFotoRequest(
        Long varianteId
) {}
