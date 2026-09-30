package com.sistventas.backend.security;

/**
 * Principal autenticado de una CUENTA DE CLIENTE (login en la tienda
 * pública) — nunca UserPrincipal. Son dos tipos de Java distintos a
 * propósito: un endpoint de panel/empresa que pide
 * {@code @AuthenticationPrincipal UserPrincipal} nunca puede recibir por
 * error un principal armado a partir de un JWT de cliente (Spring deja el
 * parámetro en null si el tipo no matchea), así que un token de cliente
 * jamás cuela en un endpoint de empresa aunque alguien se equivoque de ruta.
 * Ver JwtAuthenticationFilter (branch por el claim "tipo") y
 * JwtService.generateTokenCliente.
 */
public record ClientePrincipal(
        Long clienteId,
        Long empresaId
) {}
