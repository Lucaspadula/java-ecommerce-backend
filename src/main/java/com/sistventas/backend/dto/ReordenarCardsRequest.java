package com.sistventas.backend.dto;

import java.util.List;

// Lista COMPLETA de ids de cards del bloque en el orden deseado (orden = índice).
public record ReordenarCardsRequest(List<Long> ids) {}
