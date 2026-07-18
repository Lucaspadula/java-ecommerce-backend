package com.sistventas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guardado como FK plana (no @ManyToOne a Empresa): esta feature nunca
    // necesita navegar de Producto a Empresa, así que evitamos el join/lazy
    // loading innecesario. El scoping multiempresa filtra por este campo en
    // el repository, nunca confiando en un valor que venga del cliente.
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 150)
    private String nombre;

    // @ManyToOne (no FK plana): a diferencia de empresaId, acá SÍ hace falta
    // navegar a la categoría real para resolver su nombre (ver
    // ProductoServiceImpl.toDto) — mismo criterio que ProductoInsumo.insumo,
    // que también referencia una entidad maestra real con @ManyToOne.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    // Opcional: no todo producto tiene subcategoría cargada.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategoria_id")
    private Subcategoria subcategoria;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    // Ambos nullable a propósito: la venta por mayor es opcional. Solo se
    // aplica cuando los dos están cargados (ver VentaServiceImpl).
    @Column(name = "precio_por_mayor", precision = 12, scale = 2)
    private BigDecimal precioPorMayor;

    @Column(name = "cantidad_minima_mayorista")
    private Integer cantidadMinimaMayorista;

    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    // Slots fijos 2 y 3 de la galería del producto (no una tabla aparte con
    // "orden": son a propósito 3 columnas fijas, ver ProductoServiceImpl.actualizarFoto
    // y su parámetro slot). Se muestran solo en el detalle (admin y tienda
    // pública), nunca en miniaturas de listado/grilla — ahí siempre se usa
    // fotoUrl.
    @Column(name = "foto_url_2", length = 255)
    private String fotoUrl2;

    @Column(name = "foto_url_3", length = 255)
    private String fotoUrl3;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    // Columna de esquema sin uso: todo producto se compone de insumos, sin
    // excepción, así que el stock siempre sale de StockDisponibleCalculator
    // en base al stock de los insumos de la receta. Se deja existir por
    // compatibilidad de esquema (no vale la pena una migración solo para
    // borrarla) pero nunca se lee como fuente de verdad.
    @Column(nullable = false)
    private Integer stock = 0;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductoInsumo> insumos = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public Subcategoria getSubcategoria() { return subcategoria; }
    public void setSubcategoria(Subcategoria subcategoria) { this.subcategoria = subcategoria; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(BigDecimal precioVenta) { this.precioVenta = precioVenta; }

    public BigDecimal getPrecioPorMayor() { return precioPorMayor; }
    public void setPrecioPorMayor(BigDecimal precioPorMayor) { this.precioPorMayor = precioPorMayor; }

    public Integer getCantidadMinimaMayorista() { return cantidadMinimaMayorista; }
    public void setCantidadMinimaMayorista(Integer cantidadMinimaMayorista) { this.cantidadMinimaMayorista = cantidadMinimaMayorista; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getFotoUrl2() { return fotoUrl2; }
    public void setFotoUrl2(String fotoUrl2) { this.fotoUrl2 = fotoUrl2; }

    public String getFotoUrl3() { return fotoUrl3; }
    public void setFotoUrl3(String fotoUrl3) { this.fotoUrl3 = fotoUrl3; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDateTime fechaAlta) { this.fechaAlta = fechaAlta; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public List<ProductoInsumo> getInsumos() { return insumos; }
    public void setInsumos(List<ProductoInsumo> insumos) { this.insumos = insumos; }
}
