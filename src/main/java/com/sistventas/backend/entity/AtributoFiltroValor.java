package com.sistventas.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Valor posible de un AtributoFiltro (ej. atributo "Material" -> valores
// "Acero", "Hierro fundido"). La asignación real por producto vive en
// Producto.atributoValores (join producto_atributo_valor).
@Entity
@Table(name = "atributo_filtro_valor")
public class AtributoFiltroValor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "atributo_filtro_id", nullable = false)
    private Long atributoFiltroId;

    @Column(nullable = false, length = 100)
    private String valor;

    @Column(nullable = false)
    private Integer orden = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAtributoFiltroId() { return atributoFiltroId; }
    public void setAtributoFiltroId(Long atributoFiltroId) { this.atributoFiltroId = atributoFiltroId; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
}
