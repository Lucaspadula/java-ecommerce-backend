package com.sistventas.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Línea de un kit enviada al crear/editar un producto: referencia a otro
// Producto ya cargado (por id, scopeado a la empresa del usuario en el
// service) + cantidad de unidades que entran en el combo.
public record ProductoComponenteRequest(
        @NotNull(message = "El producto componente es obligatorio")
        Long componenteProductoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad
) {}
