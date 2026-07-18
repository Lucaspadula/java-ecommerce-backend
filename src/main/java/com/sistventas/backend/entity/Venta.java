package com.sistventas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venta")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guardado como FK plana (no @ManyToOne a Empresa), mismo criterio que
    // Cliente/Producto/Proveedor: el scoping multiempresa filtra por este
    // campo en el repository, nunca confiando en un valor que venga del
    // cliente (HTTP).
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    // FK plana también hacia Cliente: no necesitamos navegar Venta->Cliente
    // en JPA, el nombre del cliente se resuelve en el service vía
    // ClienteRepository (batch fetch en el listado para evitar N+1).
    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVenta estado = EstadoVenta.PRESUPUESTO;

    @Column(name = "fecha_pedido", nullable = false, updatable = false)
    private LocalDateTime fechaPedido;

    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    @Column(columnDefinition = "TEXT")
    private String notas;

    // Los 3 siguientes son opcionales y solo informativos: vienen del
    // formulario de confirmación de la tienda pública (ver
    // PublicPedidoRequest), no hay cálculo de costo de envío en esta fase.
    // Quedan en null para ventas creadas desde el panel admin (no pasan por
    // ese formulario).
    @Column(name = "direccion_envio", length = 200)
    private String direccionEnvio;

    @Column(length = 150)
    private String localidad;

    @Column(name = "codigo_postal", length = 20)
    private String codigoPostal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "descuento_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal descuentoPorcentaje = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // Descuento automático por combo de categorías (ver
    // CalculadorDescuentoComboService), independiente de descuentoPorcentaje
    // (el cupón): son acumulables, combo primero y cupón después sobre el
    // resto (ver PublicTiendaServiceImpl.crearPedido). Es un MONTO, no otro
    // %, porque aplica solo sobre el precio de los productos que forman cada
    // par, no sobre el total del carrito.
    @Column(name = "descuento_combo_monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuentoComboMonto = BigDecimal.ZERO;

    // Snapshot legible de qué combinación disparó el descuento (ej. "Mates +
    // Bombillas"), para mostrarlo en el pedido aunque la regla se borre o
    // edite después. Null si no se aplicó ningún combo.
    @Column(name = "descuento_combo_detalle", length = 255)
    private String descuentoComboDetalle;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VentaItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

    public EstadoVenta getEstado() { return estado; }
    public void setEstado(EstadoVenta estado) { this.estado = estado; }

    public LocalDateTime getFechaPedido() { return fechaPedido; }
    public void setFechaPedido(LocalDateTime fechaPedido) { this.fechaPedido = fechaPedido; }

    public LocalDate getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDate fechaEntrega) { this.fechaEntrega = fechaEntrega; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public String getDireccionEnvio() { return direccionEnvio; }
    public void setDireccionEnvio(String direccionEnvio) { this.direccionEnvio = direccionEnvio; }

    public String getLocalidad() { return localidad; }
    public void setLocalidad(String localidad) { this.localidad = localidad; }

    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDescuentoPorcentaje() { return descuentoPorcentaje; }
    public void setDescuentoPorcentaje(BigDecimal descuentoPorcentaje) { this.descuentoPorcentaje = descuentoPorcentaje; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public BigDecimal getDescuentoComboMonto() { return descuentoComboMonto; }
    public void setDescuentoComboMonto(BigDecimal descuentoComboMonto) { this.descuentoComboMonto = descuentoComboMonto; }

    public String getDescuentoComboDetalle() { return descuentoComboDetalle; }
    public void setDescuentoComboDetalle(String descuentoComboDetalle) { this.descuentoComboDetalle = descuentoComboDetalle; }

    public LocalDateTime getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDateTime fechaAlta) { this.fechaAlta = fechaAlta; }

    public List<VentaItem> getItems() { return items; }
    public void setItems(List<VentaItem> items) { this.items = items; }
}
