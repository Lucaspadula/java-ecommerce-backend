package com.sistventas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proveedor")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guardado como FK plana (no @ManyToOne a Empresa), mismo criterio que
    // Cliente/Producto: esta feature nunca necesita navegar de Proveedor a
    // Empresa, así que evitamos el join/lazy loading innecesario. El scoping
    // multiempresa filtra por este campo en el repository, nunca confiando
    // en un valor que venga del cliente (HTTP).
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 190)
    private String contacto;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pedido", nullable = false, length = 20)
    private EstadoPedido estadoPedido = EstadoPedido.SIN_PEDIDO;

    @Column(name = "detalle_pedido_actual", columnDefinition = "TEXT")
    private String detallePedidoActual;

    @Column(name = "fecha_pedido")
    private LocalDate fechaPedido;

    @Column(name = "fecha_llegada_estimada")
    private LocalDate fechaLlegadaEstimada;

    @Column(name = "ultimo_pedido_detalle", columnDefinition = "TEXT")
    private String ultimoPedidoDetalle;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public EstadoPedido getEstadoPedido() { return estadoPedido; }
    public void setEstadoPedido(EstadoPedido estadoPedido) { this.estadoPedido = estadoPedido; }

    public String getDetallePedidoActual() { return detallePedidoActual; }
    public void setDetallePedidoActual(String detallePedidoActual) { this.detallePedidoActual = detallePedidoActual; }

    public LocalDate getFechaPedido() { return fechaPedido; }
    public void setFechaPedido(LocalDate fechaPedido) { this.fechaPedido = fechaPedido; }

    public LocalDate getFechaLlegadaEstimada() { return fechaLlegadaEstimada; }
    public void setFechaLlegadaEstimada(LocalDate fechaLlegadaEstimada) { this.fechaLlegadaEstimada = fechaLlegadaEstimada; }

    public String getUltimoPedidoDetalle() { return ultimoPedidoDetalle; }
    public void setUltimoPedidoDetalle(String ultimoPedidoDetalle) { this.ultimoPedidoDetalle = ultimoPedidoDetalle; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDateTime fechaAlta) { this.fechaAlta = fechaAlta; }
}
