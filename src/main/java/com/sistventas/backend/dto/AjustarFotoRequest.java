package com.sistventas.backend.dto;

// Body de PATCH /api/productos/{id}/fotos/{fotoId}/ajuste: toggle manual del
// ajuste de una foto (ver spec "Ajuste manual de foto"). false = se ve
// completa (contain), true = se agranda llenando el marco aunque recorte
// bordes (cover).
public record AjustarFotoRequest(
        boolean agrandada
) {}
