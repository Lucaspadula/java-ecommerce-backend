package com.sistventas.backend.service.impl;

import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ProductoInsumo;
import com.sistventas.backend.entity.RolInsumo;
import org.springframework.stereotype.Component;

import java.util.List;

// Producto vendido DENTRO de un kit: el kit pone el embalaje compartido, así
// que del componente solo se consume la materia prima real. El filtro es "no
// EMBALAJE" a propósito (y no un allowlist de MATERIA_PRIMA): los insumos
// con rol null (sin clasificar todavía — la reclasificación de datos es un
// trabajo manual pendiente del dueño) se comportan como materia prima y se
// siguen consumiendo, así nada deja de descontarse por accidente hasta que
// el dueño los clasifique.
@Component
public class ConsumoEnComboStrategy implements EstrategiaConsumoReceta {

    @Override
    public List<ProductoInsumo> resolver(Producto producto) {
        return producto.getInsumos().stream()
                .filter(linea -> linea.getInsumo().getRol() != RolInsumo.EMBALAJE)
                .toList();
    }
}
