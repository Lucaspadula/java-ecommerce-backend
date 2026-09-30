package com.sistventas.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Un punto/párrafo de las páginas "Importante" (políticas) o "Cómo comprar"
// del catálogo generado en PDF (ver CatalogoServiceImpl.generarPdf). Lista
// libre por empresa: el admin agrega y saca puntos uno por uno, mismo
// criterio de tabla propia + `orden` que TiendaTip, distinguiendo la lista a
// la que pertenece cada fila con `tipo` (mismo patrón que
// TiendaBannerImagen.tipo con HERO/VERTICAL).
@Entity
@Table(name = "tienda_catalogo_seccion")
public class TiendaCatalogoSeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    // Valores válidos: "IMPORTANTE", "COMO_COMPRAR" (ver
    // TiendaCatalogoSeccionServiceImpl). String plano, no enum: mismo
    // criterio que TiendaBannerImagen.tipo.
    @Column(nullable = false, length = 20)
    private String tipo;

    // TEXT (no VARCHAR): el admin escribe el bloque como quiere — con sus
    // propios saltos de línea y espacios — y se muestra tal cual en el PDF,
    // sin reformatear ni numerar automáticamente (ver
    // CatalogoServiceImpl.agregarBloquesDeTexto).
    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    // Orden de aparición dentro de su lista (tipo). Se asigna al crear como
    // "cantidad actual de esa lista", mismo criterio que TiendaTip.orden.
    @Column(nullable = false)
    private Integer orden = 0;

    // Estilo POR BLOQUE (V58) — a diferencia de Empresa.catalogo_texto_*
    // (V57, fuente/tamaño/color GLOBAL para TODOS los puntos de ambas
    // páginas), esto deja destacar un punto puntual distinto del resto:
    // alinearlo, ponerlo en negrita, etc. Valores válidos de alineacion:
    // "IZQUIERDA", "CENTRO", "DERECHA" (ver
    // TiendaCatalogoSeccionServiceImpl.alineacionValidaOThrow).
    @Column(nullable = false, length = 10)
    private String alineacion = "IZQUIERDA";

    @Column(nullable = false)
    private boolean negrita = false;

    @Column(nullable = false)
    private boolean cursiva = false;

    @Column(nullable = false)
    private boolean subrayado = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public String getAlineacion() { return alineacion; }
    public void setAlineacion(String alineacion) { this.alineacion = alineacion; }

    public boolean isNegrita() { return negrita; }
    public void setNegrita(boolean negrita) { this.negrita = negrita; }

    public boolean isCursiva() { return cursiva; }
    public void setCursiva(boolean cursiva) { this.cursiva = cursiva; }

    public boolean isSubrayado() { return subrayado; }
    public void setSubrayado(boolean subrayado) { this.subrayado = subrayado; }
}
