package com.sistventas.backend.service.impl;

import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ProductoInsumo;
import org.springframework.stereotype.Component;

import java.util.List;

// Producto vendido suelto: consume su receta completa, embalaje incluido —
// exactamente el comportamiento que el sistema tenía antes de existir los
// kits, sin ningún cambio.
@Component
public class ConsumoDirectoStrategy implements EstrategiaConsumoReceta {

    @Override
    public List<ProductoInsumo> resolver(Producto producto) {
        return producto.getInsumos();
    }
}
