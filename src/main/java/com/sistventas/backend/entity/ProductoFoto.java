package com.sistventas.backend.entity;

import jakarta.persistence.*;

// Una foto del pool unificado de un producto (hasta 8, ver spec "Tope de
// pool por producto"). `variante` nullable: sin color = "general" (se
// muestra en la grilla/vidriera para cualquier color), con color = solo se
// muestra al elegir esa variante puntual (ver ProductoFotoResolver). Sin FK a
// variante (mismo criterio que VentaItem.varianteId, V34): una
// ProductoVariante SÍ puede borrarse físicamente (orphanRemoval en
// Producto.variantes), una FK acá impediría borrar un color discontinuado
// que ya tuviera fotos propias en el pool.
@Entity
@Table(name = "producto_foto")
public class ProductoFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    // Sin @ManyToOne a propósito (ver comentario de clase): se guarda el id
    // plano, igual que VentaItem.varianteId.
    @Column(name = "variante_id")
    private Long varianteId;

    @Column(nullable = false, length = 255)
    private String url;

    @Column(nullable = false)
    private int orden;

    // Toggle manual del dueño (ver spec "Ajuste manual de foto"): false =
    // se ve completa (contain), true = se agranda para llenar el marco
    // aunque recorte bordes (cover). Antes esto lo decidía el CSS solo.
    @Column(nullable = false)
    private boolean agrandada;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Long getVarianteId() { return varianteId; }
    public void setVarianteId(Long varianteId) { this.varianteId = varianteId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }

    public boolean isAgrandada() { return agrandada; }
    public void setAgrandada(boolean agrandada) { this.agrandada = agrandada; }
}
