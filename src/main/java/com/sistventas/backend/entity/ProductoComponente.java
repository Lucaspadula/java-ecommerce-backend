package com.sistventas.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "producto_componente")
public class ProductoComponente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El kit (Composite): el producto que agrupa a otros. Mismo patrón de
    // línea-de-receta que ProductoInsumo, pero referenciando un Producto
    // terminado en vez de un Insumo — antes de esta tabla, un combo se
    // armaba con un Insumo como sustituto del producto (un hack, ver
    // V33__producto_componente.sql).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "componente_producto_id", nullable = false)
    private Producto componenteProducto;

    // Entera (no decimal como ProductoInsumo.cantidad): un kit incluye
    // unidades enteras de productos terminados, nunca fracciones.
    @Column(nullable = false)
    private int cantidad = 1;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Producto getComponenteProducto() { return componenteProducto; }
    public void setComponenteProducto(Producto componenteProducto) { this.componenteProducto = componenteProducto; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
