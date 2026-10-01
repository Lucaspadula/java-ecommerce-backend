package com.sistventas.backend.dto;

import java.util.List;

// Lista COMPLETA de ids de bloques del slot en el orden deseado (orden = índice).
public record ReordenarBloquesRequest(String slot, List<Long> ids) {}
