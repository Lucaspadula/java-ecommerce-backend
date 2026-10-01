package com.sistventas.backend.dto;

// destino ya resuelto por el backend: id de producto, NOMBRE de categoría
// (el catálogo filtra por ?categoria=<nombre>) o URL; null en NINGUNA/MODAL.
public record PublicTiendaBloqueCardDto(
        Long id,
        String imagenUrl,
        String orientacion,
        String titulo,
        String texto,
        String accion,
        String destino
) {}
