package com.sistventas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "venta_item")
public class VentaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    // FK plana hacia Producto (no @ManyToOne): el item ya guarda un snapshot
    // de nombre/precio, así que no necesita navegar en vivo al producto. Se
    // usa solo para trazabilidad y para revalidar en updates futuros.
    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    // Snapshot del nombre al momento de la venta: si después editan o
    // desactivan el producto, el item histórico no debe cambiar.
    @Column(name = "producto_nombre", nullable = false, length = 150)
    private String productoNombre;

    @Column(nullable = false)
    private int cantidad;

    // Snapshot del precio, no referencia en vivo al precio actual del
    // producto.
    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(columnDefinition = "TEXT")
    private String personalizacion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    // Foto de referencia opcional para la personalización/grabado. Se sube
    // antes de que el item exista como fila (ver VentaServiceImpl.subirFoto)
    // y la url viaja como parte del payload normal de crear/actualizar.
    @Column(name = "foto_url")
    private String fotoUrl;

    // Qué variante de color puntual se vendió (nullable): null para
    // productos sin variantes, comportamiento de siempre sin cambios. FK
    // plana sin @ManyToOne ni constraint de BD a propósito — ver
    // V34__producto_variante.sql. Determina si VentaServiceImpl descuenta el
    // stock de la variante puntual o el del producto (ver
    // VentaServiceImpl.demandaPorVariante).
    @Column(name = "variante_id")
    private Long varianteId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public String getProductoNombre() { return productoNombre; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public String getPersonalizacion() { return personalizacion; }
    public void setPersonalizacion(String personalizacion) { this.personalizacion = personalizacion; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public Long getVarianteId() { return varianteId; }
    public void setVarianteId(Long varianteId) { this.varianteId = varianteId; }
}
