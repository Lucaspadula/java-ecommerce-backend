package com.sistventas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "insumo")
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guardado como FK plana (no @ManyToOne a Empresa), mismo criterio que
    // Producto/Cliente: esta feature nunca necesita navegar de Insumo a
    // Empresa, así que evitamos el join/lazy loading innecesario. El scoping
    // multiempresa filtra por este campo en el repository, nunca confiando
    // en un valor que venga del cliente (HTTP).
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "costo_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal stock;

    @Column(name = "stock_minimo", precision = 12, scale = 3)
    private BigDecimal stockMinimo;

    @Column(name = "unidad_medida", length = 30)
    private String unidadMedida;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    // Nullable a propósito: no todo insumo existente está clasificado
    // todavía (ver V32__insumo_rol.sql y RolInsumo). Consumido por
    // ConsumoEnComboStrategy para saltear EMBALAJE cuando el producto se
    // vende como componente de un Kit.
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", length = 20)
    private RolInsumo rol;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }

    public BigDecimal getStock() { return stock; }
    public void setStock(BigDecimal stock) { this.stock = stock; }

    public BigDecimal getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(BigDecimal stockMinimo) { this.stockMinimo = stockMinimo; }

    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDateTime fechaAlta) { this.fechaAlta = fechaAlta; }

    public RolInsumo getRol() { return rol; }
    public void setRol(RolInsumo rol) { this.rol = rol; }
}
