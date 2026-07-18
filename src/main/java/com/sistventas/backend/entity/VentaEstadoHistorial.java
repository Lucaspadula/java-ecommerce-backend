package com.sistventas.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "venta_estado_historial")
public class VentaEstadoHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venta_id", nullable = false)
    private Long ventaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", nullable = false, length = 20)
    private EstadoVenta estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoVenta estadoNuevo;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVentaId() { return ventaId; }
    public void setVentaId(Long ventaId) { this.ventaId = ventaId; }

    public EstadoVenta getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoVenta estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public EstadoVenta getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(EstadoVenta estadoNuevo) { this.estadoNuevo = estadoNuevo; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
