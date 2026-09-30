package com.sistventas.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Atributo de filtro personalizado, definido por el dueño POR CATEGORÍA (ej.
// categoría "Sartenes" -> atributo "Material") — ver AtributoFiltroValor para
// los valores posibles de este atributo, y Producto.atributoValores para la
// asignación real por producto.
@Entity
@Table(name = "atributo_filtro")
public class AtributoFiltro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Integer orden = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
}
