package com.sistventas.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Tip de cuidado del mate, cargado a mano por el dueño desde el panel de
// administración (título + contenido, con foto opcional, sin autor),
// mostrado en una franja fija de la home de la tienda pública, después de
// los testimonios. A diferencia de TiendaTestimonio, no hay una persona
// asociada al contenido — no tiene clienteNombre.
@Entity
@Table(name = "tienda_tip")
public class TiendaTip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String contenido;

    // Orden de aparición en la franja pública. Se asigna al crear como
    // "cantidad actual de tips de la empresa" (ver TiendaTipServiceImpl.crear),
    // mismo criterio que TiendaTestimonio.orden.
    @Column(nullable = false)
    private Integer orden = 0;

    // Foto opcional (mismo patrón que TiendaTestimonio.fotoUrl): si no está
    // cargada, la tienda pública muestra un rectángulo neutro en su lugar
    // (ver tienda-publica.css .tip-card).
    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
}
