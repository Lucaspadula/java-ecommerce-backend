package com.sistventas.backend.dto;

import java.time.LocalDateTime;

public record ResenaDto(
        Long id,
        String clienteNombre,
        String comentario,
        Integer puntuacion,
        LocalDateTime fecha,
        String imagenUrl,
        // true = reseña de un cliente con compra verificada (ventaId
        // presente en la entity). false = cargada a mano por el admin —
        // el frontend no le muestra estrellas reales en ese caso (ver
        // comentario de Resena.puntuacion).
        boolean compraVerificada
) {}
