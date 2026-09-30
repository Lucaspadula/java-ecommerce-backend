package com.sistventas.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

// Reseña de un producto: cargada a mano por el dueño desde el panel de
// administración (ventaId null, comentario obligatorio en ese flujo — ver
// CrearResenaRequest), o dejada por un cliente real con compra verificada
// (ventaId presente, sin login — ver PublicTiendaServiceImpl.crearResenaCliente,
// que valida venta entregada + teléfono + que el producto esté en esa venta
// antes de crearla).
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

    // Opcional: el cliente puede dejar solo la puntuación, sin texto. Las
    // reseñas cargadas a mano por el admin siguen exigiendo comentario (ver
    // CrearResenaRequest), pero a nivel columna ya no es NOT NULL.
    @Column(length = 1000)
    private String comentario;

    // 1 a 5. Default 5 a nivel columna (ver V50) para las reseñas viejas del
    // admin que no tenían rating — nunca se muestra ese default como si el
    // cliente lo hubiera elegido, porque el frontend solo pinta estrellas
    // reales cuando la reseña vino del flujo de compra verificada (ventaId
    // presente).
    @Column(nullable = false)
    private Integer puntuacion = 5;

    // Null = reseña cargada a mano por el admin. Presente = reseña de un
    // cliente con compra verificada, referencia a la Venta que la habilitó
    // (usado para bloquear una segunda reseña de la misma compra — ver
    // ResenaRepository.existsByVentaIdAndProductoId).
    @Column(name = "venta_id")
    private Long ventaId;

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

    public Integer getPuntuacion() { return puntuacion; }
    public void setPuntuacion(Integer puntuacion) { this.puntuacion = puntuacion; }

    public Long getVentaId() { return ventaId; }
    public void setVentaId(Long ventaId) { this.ventaId = ventaId; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
}
