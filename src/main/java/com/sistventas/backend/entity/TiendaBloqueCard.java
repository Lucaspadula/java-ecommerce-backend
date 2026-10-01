package com.sistventas.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Card de un TiendaBloque: imagen + título/texto opcionales + acción al hacer
// click (NINGUNA | MODAL | PRODUCTO | CATEGORIA | URL). bloqueId es FK plana;
// orientacion y accion son String validados en el service.
@Entity
@Table(name = "tienda_bloque_card")
public class TiendaBloqueCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bloque_id", nullable = false)
    private Long bloqueId;

    // Nullable solo por los tips legacy sin foto; el request del admin la exige.
    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(nullable = false, length = 20)
    private String orientacion = "VERTICAL";

    @Column(length = 150)
    private String titulo;

    @Column(length = 500)
    private String texto;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(nullable = false, length = 20)
    private String accion = "NINGUNA";

    // PRODUCTO/CATEGORIA: id numérico; URL: la URL http(s); resto: null.
    @Column(name = "accion_valor", length = 500)
    private String accionValor;

    // BANNER:{id} / TIP:{id} en cards migradas; null en las creadas desde el panel.
    @Column(name = "origen_legacy", length = 40)
    private String origenLegacy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBloqueId() { return bloqueId; }
    public void setBloqueId(Long bloqueId) { this.bloqueId = bloqueId; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public String getOrientacion() { return orientacion; }
    public void setOrientacion(String orientacion) { this.orientacion = orientacion; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getAccionValor() { return accionValor; }
    public void setAccionValor(String accionValor) { this.accionValor = accionValor; }

    public String getOrigenLegacy() { return origenLegacy; }
    public void setOrigenLegacy(String origenLegacy) { this.origenLegacy = origenLegacy; }
}
