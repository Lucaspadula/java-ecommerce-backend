package com.sistventas.backend.dto;

import java.util.List;

public record AtributoFiltroDto(
        Long id,
        Long categoriaId,
        String nombre,
        List<AtributoFiltroValorDto> valores
) {}
