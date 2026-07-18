package com.sistventas.backend.entity;

import jakarta.persistence.*;

/**
 * Metadata visual (color + imagen) de una categoría de producto, para el
 * carrusel "Explorá por categoría" de la tienda pública. Se relaciona con la
 * categoría real por categoriaId (FK plana a Categoria, ver entity
 * Categoria) — antes matcheaba por nombre de texto libre, lo que rompía si
 * el nombre cambiaba o tenía variaciones de mayúsculas/espacios.
 */
@Entity
@Table(name = "tienda_categoria")
public class TiendaCategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guardado como FK plana (no @ManyToOne a Empresa), mismo criterio que
    // Producto/Cliente: esta feature nunca necesita navegar de
    // TiendaCategoria a Empresa. El scoping multiempresa filtra por este
    // campo en el repository, nunca confiando en un valor que venga del
    // cliente (HTTP).
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    // FK plana a Categoria: el nombre para mostrar se resuelve consultando
    // Categoria (ver PublicTiendaServiceImpl/TiendaCategoriaServiceImpl), no
    // se duplica acá.
    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    @Column(length = 7)
    private String color;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
}
