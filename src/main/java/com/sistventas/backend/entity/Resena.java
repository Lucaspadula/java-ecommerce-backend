package com.sistventas.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

// Testimonio de un cliente cargado a mano por el dueño desde el panel de
// administración (ej. copia algo que le dijeron por WhatsApp) para mostrar
// en la vidriera pública del producto. No es un sistema de reviews público:
// no hay calificación por estrellas y no existe ningún endpoint de
// escritura sin login — solo el dueño carga reseñas.
@Entity
@Table(name = "resena")
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "cliente_nombre", nullable = false, length = 150)
    private String clienteNombre;

    @Column(nullable = false, length = 1000)
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    // Captura de WhatsApp opcional como respaldo visual del comentario (que
    // sigue siendo obligatorio) — no reemplaza el texto, solo lo acompaña.
    // Se sube después, con un endpoint multipart aparte (ver
    // ProductoController.actualizarFotoResena), mismo patrón que
    // Producto.fotoUrl.
    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
}
