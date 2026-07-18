package com.sistventas.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "producto_variante")
public class ProductoVariante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Variantes de color (Etapa 1): a diferencia de ProductoComponente/
    // ProductoInsumo, esta fila ES la fuente de stock (VentaServiceImpl la
    // descuenta directo vía VentaItem.varianteId), no una línea de receta
    // sobre otra entidad. Por eso ProductoServiceImpl la actualiza con un
    // merge por id en vez de reemplazo completo — ver comentario ahí.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    // Texto libre (no catálogo maestro): a diferencia de Insumo/Categoria, el
    // color no necesita normalización ni reutilización entre productos
    // distintos, alcanza con lo que cargue el dueño acá.
    @Column(nullable = false, length = 60)
    private String color;

    @Column(nullable = false)
    private int stock = 0;

    // Nullable a propósito: la foto por variante recién se usa en la tienda
    // pública (Etapa 2, selector de color al comprar), acá solo se guarda el
    // dato si el dueño ya lo carga desde el admin.
    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
}
