package com.sistventas.backend.entity;

// Clasifica qué rol cumple un Insumo dentro de la receta de un Producto.
// Insumo.rol es nullable a propósito: los insumos existentes no están
// clasificados todavía (reclasificación manual pendiente, aparte). Ver
// ConsumoEnComboStrategy: solo EMBALAJE se saltea al vender un producto como
// componente de un Kit — MATERIA_PRIMA y null (sin clasificar todavía) se
// consumen igual, el filtro es "no EMBALAJE", no un allowlist.
public enum RolInsumo {
    MATERIA_PRIMA,
    EMBALAJE
}
