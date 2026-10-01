package com.sistventas.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Bloque configurable de la tienda pública: vive en un slot (punto de anclaje
// de una pantalla), tiene un ancho proporcional y contiene cards
// (TiendaBloqueCard). slot y ancho son String validados contra
// TiendaBloqueCatalogo en TiendaBloqueServiceImpl (convención del proyecto: sin
// enums de BD). empresaId es FK plana, sin @ManyToOne.
@Entity
@Table(name = "tienda_bloque")
public class TiendaBloque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(length = 150)
    private String titulo;

    @Column(nullable = false, length = 40)
    private String slot;

    @Column(nullable = false, length = 20)
    private String ancho = "COMPLETO";

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    // Marcador de bloques migrados desde los tips / banners verticales
    // (BANNER_VERTICAL, TIPS); null en los bloques creados desde el panel.
    @Column(name = "origen_legacy", length = 40)
    private String origenLegacy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public String getAncho() { return ancho; }
    public void setAncho(String ancho) { this.ancho = ancho; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public String getOrigenLegacy() { return origenLegacy; }
    public void setOrigenLegacy(String origenLegacy) { this.origenLegacy = origenLegacy; }
}
