package com.sistventas.backend.entity;

import jakarta.persistence.*;

// Catálogo maestro de categorías de producto, por empresa. Reemplaza al
// viejo Producto.categoria (texto libre): antes dos productos "Mates" y
// "mates " convivían como strings distintos sin ninguna relación real (fuera
// de 3FN). Ahora cada empresa tiene su propio set de filas acá, y Producto
// referencia una por id (ver Producto.categoria).
@Entity
@Table(name = "categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guardado como FK plana (no @ManyToOne a Empresa), mismo criterio que
    // Producto/Insumo: esta feature nunca necesita navegar de Categoria a
    // Empresa. El scoping multiempresa filtra por este campo en el
    // repository, nunca confiando en un valor que venga del cliente (HTTP).
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 100)
    private String nombre;

    // Orden de presentación manual (ej. en selects/nav de categorías). 0 por
    // default: hoy nada arma un reorder UI todavía, se deja el campo listo
    // sin uso real más allá del orden alfabético que ya aplican los
    // repositories (ver CategoriaRepository).
    @Column(nullable = false)
    private int orden = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
