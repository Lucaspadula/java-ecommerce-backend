package com.sistventas.backend.entity;

import jakarta.persistence.*;

// Subcategoría maestra, siempre hija de una Categoria (ver categoriaId).
// Reemplaza al viejo Producto.subcategoria (texto libre). Guardado como FK
// plana a Categoria (no @ManyToOne): a diferencia de Producto -> Categoria
// (que sí necesita navegar para resolver el nombre en los DTOs), acá el
// único uso es filtrar/validar "esta subcategoría pertenece a esta
// categoría" desde el repository, mismo criterio que empresaId en Categoria.
@Entity
@Table(name = "subcategoria")
public class Subcategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    @Column(nullable = false, length = 100)
    private String nombre;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
