package com.sistventas.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Testimonio general del negocio, cargado a mano por el dueño desde el panel
// de administración (nombre + comentario, con foto y canal opcionales),
// mostrado en una franja fija de la home de la tienda pública. A diferencia
// de Resena, NO está ligado a un producto puntual — no tiene productoId.
@Entity
@Table(name = "tienda_testimonio")
public class TiendaTestimonio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "cliente_nombre", nullable = false, length = 150)
    private String clienteNombre;

    @Column(nullable = false, length = 500)
    private String comentario;

    // Orden de aparición en la franja pública. Se asigna al crear como
    // "cantidad actual de testimonios de la empresa" (ver
    // TiendaTestimonioServiceImpl.crear), mismo criterio que
    // TiendaBannerImagen.orden.
    @Column(nullable = false)
    private Integer orden = 0;

    // Respaldo visual opcional del testimonio (ej. captura de WhatsApp o
    // foto del cliente), subido después con un endpoint multipart aparte
    // (ver TiendaTestimonioServiceImpl.actualizarFoto), mismo patrón que
    // Resena.imagenUrl.
    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    // Opcional: el admin puede no elegir canal al cargar el testimonio.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CanalTestimonio canal;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public CanalTestimonio getCanal() { return canal; }
    public void setCanal(CanalTestimonio canal) { this.canal = canal; }
}
