package com.sistventas.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Etapa 1 de inventario: Insumo dejó de ser texto suelto embebido en cada
// Producto y pasó a ser una entidad maestra propia, con su propio CRUD y
// stock. Este DTO representa ese maestro (no una línea de receta dentro de
// un producto — para eso está ProductoInsumoDto).
public record InsumoDto(
        Long id,
        String nombre,
        BigDecimal costoUnitario,
        BigDecimal stock,
        BigDecimal stockMinimo,
        String unidadMedida,
        boolean activo,
        LocalDateTime fechaAlta
) {}
