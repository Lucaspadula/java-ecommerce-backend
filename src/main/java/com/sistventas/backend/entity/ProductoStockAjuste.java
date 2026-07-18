package com.sistventas.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "producto_stock_ajuste")
public class ProductoStockAjuste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    // FK plana a empresa, mismo patrón de scoping multiempresa que Producto:
    // redundante con producto_id pero evita tener que joinear a producto
    // para saber a qué empresa pertenece un ajuste.
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false)
    private Integer delta;

    @Column(name = "stock_resultante", nullable = false)
    private Integer stockResultante;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public Integer getDelta() { return delta; }
    public void setDelta(Integer delta) { this.delta = delta; }

    public Integer getStockResultante() { return stockResultante; }
    public void setStockResultante(Integer stockResultante) { this.stockResultante = stockResultante; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
