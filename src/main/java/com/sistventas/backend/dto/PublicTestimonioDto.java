package com.sistventas.backend.dto;

import com.sistventas.backend.entity.CanalTestimonio;

// Solo lectura, sin id ni orden (ya vienen ordenados en la lista): mismo
// criterio que ResenaDto no exponer nada que el visitante de la tienda no
// necesite.
public record PublicTestimonioDto(
        String clienteNombre,
        String comentario,
        String fotoUrl,
        CanalTestimonio canal
) {}
