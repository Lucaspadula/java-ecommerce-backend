package com.sistventas.backend.service.impl;

import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ProductoInsumo;

import java.util.List;

// Strategy (GoF): un mismo producto consume su receta de forma distinta
// según el contexto de venta. Vendido suelto consume TODO (incluido su
// embalaje propio); vendido como componente de un kit, el kit pone su propio
// embalaje compartido, así que el componente saltea sus insumos de rol
// EMBALAJE — sin esto, un combo Mate+Bombilla descontaría dos bolsas y dos
// stickers cuando físicamente se arma con uno solo (el del combo).
// Ver ConsumoDirectoStrategy / ConsumoEnComboStrategy.
public interface EstrategiaConsumoReceta {

    List<ProductoInsumo> resolver(Producto producto);
}
