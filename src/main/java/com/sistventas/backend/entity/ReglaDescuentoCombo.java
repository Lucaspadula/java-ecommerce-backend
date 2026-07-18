package com.sistventas.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

// Regla de descuento automático por combinación de categorías (ej. "Mates +
// Bombillas"), configurada a mano por el admin desde el panel. Se aplica
// sola cuando el carrito del cliente tiene, a la vez, un producto que
// matchea el lado A y uno que matchea el lado B — no es un combo armado a
// mano por el cliente. Ver CalculadorDescuentoComboService para el motor de
// cálculo (pares completos + "mejor regla gana").
@Entity
@Table(name = "regla_descuento_combo")
public class ReglaDescuentoCombo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    // FK plana a Categoria (no @ManyToOne): CalculadorDescuentoComboService
    // solo necesita comparar ids, nunca navega a Categoria desde acá (los
    // nombres para mostrar se resuelven en ReglaDescuentoComboServiceImpl).
    @Column(name = "categoria_a_id", nullable = false)
    private Long categoriaAId;

    // Opcional: si es null, el lado A matchea cualquier producto de
    // categoriaAId sin importar la subcategoría (ver
    // CalculadorDescuentoComboService.matchea).
    @Column(name = "subcategoria_a_id")
    private Long subcategoriaAId;

    @Column(name = "categoria_b_id", nullable = false)
    private Long categoriaBId;

    @Column(name = "subcategoria_b_id")
    private Long subcategoriaBId;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @Column(nullable = false)
    private boolean activo = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public Long getCategoriaAId() { return categoriaAId; }
    public void setCategoriaAId(Long categoriaAId) { this.categoriaAId = categoriaAId; }

    public Long getSubcategoriaAId() { return subcategoriaAId; }
    public void setSubcategoriaAId(Long subcategoriaAId) { this.subcategoriaAId = subcategoriaAId; }

    public Long getCategoriaBId() { return categoriaBId; }
    public void setCategoriaBId(Long categoriaBId) { this.categoriaBId = categoriaBId; }

    public Long getSubcategoriaBId() { return subcategoriaBId; }
    public void setSubcategoriaBId(Long subcategoriaBId) { this.subcategoriaBId = subcategoriaBId; }

    public BigDecimal getPorcentaje() { return porcentaje; }
    public void setPorcentaje(BigDecimal porcentaje) { this.porcentaje = porcentaje; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
