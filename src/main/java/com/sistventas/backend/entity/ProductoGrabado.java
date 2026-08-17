package com.sistventas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "producto_grabado")
public class ProductoGrabado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    // Texto libre (no catálogo maestro), mismo criterio que
    // ProductoVariante.color: el lugar grabable varía tanto de un producto a
    // otro (Virola, Cuerpo, Tapa...) que no vale la pena normalizarlo.
    @Column(nullable = false, length = 60)
    private String lugar;

    // Precio del servicio en ESE lugar puntual — un mismo producto puede
    // tener varios lugares grabables a precios distintos (ej. Virola más
    // barato que grabar todo el cuerpo de algarrobo).
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
}
