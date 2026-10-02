package com.sistventas.backend.entity;

import jakarta.persistence.*;

/**
 * Una imagen del carrusel de banner de la tienda pública (rotación
 * automática con Ken Burns + crossfade, ver PublicTiendaServiceImpl y
 * tienda-publica.ts). Guardada como FK plana (no @ManyToOne a Empresa),
 * mismo criterio que TiendaCategoria: el scoping multiempresa filtra por
 * este campo en el repository, nunca confiando en un valor que venga del
 * cliente (HTTP).
 */
@Entity
@Table(name = "tienda_banner_imagen")
public class TiendaBannerImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "imagen_url", nullable = false, length = 500)
    private String imagenUrl;

    @Column(nullable = false)
    private int orden;

    // Hoy siempre "HERO" (banner rotativo): los VERTICAL se retiraron en V64
    // (pasaron a bloques). Se conserva la columna para no tocar el HERO.
    // String simple, no un enum de JPA: mismo criterio que Empresa.tiendaFuente.
    @Column(nullable = false, length = 20)
    private String tipo = "HERO";

    // Producto opcional asociado a esta imagen: al clickear el banner en la
    // tienda pública, si tiene productoId, se abre el modal de detalle de
    // ESE producto. Null = el click no hace nada especial (comportamiento
    // actual). FK plana igual que empresaId: el scoping multiempresa se
    // valida siempre en el service (nunca confiar en un id que venga del
    // cliente sin chequear que el producto sea de la misma empresa).
    @Column(name = "producto_id")
    private Long productoId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
}
