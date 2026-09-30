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

    // DEPRECADA junto con fotoUrl2/3/4 (ver comentario de abajo): el DTO
    // sigue exponiendo un campo `fotoUrl` escalar, pero ahora es DERIVADO vía
    // ProductoFotoResolver.resolverMiniatura(fotos), no esta columna.
    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    // DEPRECADAS (V43__producto_foto_pool.sql): reemplazadas por el pool
    // unificado `fotos` de abajo. Se dejan existir en el esquema sin
    // eliminarse (la migración es no destructiva a propósito, ver
    // Migration/Rollout en el design) pero YA NO SE LEEN NI ESCRIBEN desde
    // ningún camino de código nuevo — mismo criterio de "columna deprecada,
    // no vale la pena una migración solo para borrarla" que
    // ProductoVariante.precioVenta. El DROP queda diferido a una V44 futura.
    @Column(name = "foto_url_2", length = 255)
    private String fotoUrl2;

    @Column(name = "foto_url_3", length = 255)
    private String fotoUrl3;

    @Column(name = "foto_url_4", length = 255)
    private String fotoUrl4;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    // Stock propio: SOLO se usa (se lee y se mueve en ventas/ajustes) cuando
    // el producto no tiene receta de insumos ni componentes de kit — ver
    // StockDisponibleCalculator.calcular y VentaServiceImpl.demandaStockPropio.
    // Para un producto CON receta o kit, esta columna queda inerte (se sigue
    // persistiendo lo que mande el form, pero nadie la lee como fuente de
    // verdad): el disponible sale siempre del cálculo correspondiente.
    @Column(nullable = false)
    private Integer stock = 0;

    // Costo propio de producir/comprar ESTE producto, opcional. Se suma
    // siempre al costo de la receta de insumos (embalaje, stickers, etc.) —
    // no es excluyente con ella (ver ProductoServiceImpl.costoEfectivo).
    @Column(name = "costo_unitario", precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductoInsumo> insumos = new ArrayList<>();

    // Composite: un producto con componentes es un "kit" que agrupa otros
    // productos terminados (ej. Combo = 1 Mate + 1 Termo). Lista vacía =
    // producto simple, comportamiento de siempre — TODO el código de
    // stock/venta ramifica explícitamente en ese chequeo antes de tocar el
    // camino del kit (ver VentaServiceImpl y StockDisponibleCalculator).
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductoComponente> componentes = new ArrayList<>();

    // Variantes de color (Etapa 1): lista vacía = producto simple, stock
    // propio de siempre (campo `stock` de arriba), sin ningún cambio — mismo
    // criterio de ramificar explícitamente que insumos/componentes (ver
    // StockDisponibleCalculator y VentaServiceImpl). La receta de insumos de
    // este producto (si tiene) sigue siendo COMPARTIDA entre todas las
    // variantes: no se duplica por color, solo el stock puntual se cuenta
    // aparte por variante.
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductoVariante> variantes = new ArrayList<>();

    // Lugares grabables (ej. "Virola", "Cuerpo de algarrobo"), cada uno con
    // su propio precio de servicio. Lista vacía = producto sin opción de
    // grabado, no se muestra el selector en la tienda pública.
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductoGrabado> grabados = new ArrayList<>();

    // Pool unificado de fotos (V43__producto_foto_pool.sql, reemplaza
    // fotoUrl/2/3/4 de arriba): hasta 8 por producto, cada una con
    // variante_id opcional (color). orphanRemoval=true: borrar una foto del
    // form la borra físicamente de la fila, no soft-delete (mismo criterio
    // que insumos/componentes/grabados, no como Producto que se da de baja
    // lógica).
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductoFoto> fotos = new ArrayList<>();

    // Valores de atributo de filtro asignados a ESTE producto (ej. Material =
    // "Acero"), a lo sumo uno por AtributoFiltro — ver AtributoFiltroValor.
    // @ManyToMany simple (no entity de asociación propia): la única
    // información de la fila es el par (producto, valor), no hace falta
    // nada más ahí. Lista vacía = producto sin ningún atributo asignado,
    // no aparece en ningún filtro custom de la tienda pública.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "producto_atributo_valor",
            joinColumns = @JoinColumn(name = "producto_id"),
            inverseJoinColumns = @JoinColumn(name = "atributo_filtro_valor_id"))
    private List<AtributoFiltroValor> atributoValores = new ArrayList<>();

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

    public String getFotoUrl4() { return fotoUrl4; }
    public void setFotoUrl4(String fotoUrl4) { this.fotoUrl4 = fotoUrl4; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDateTime fechaAlta) { this.fechaAlta = fechaAlta; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }

    public List<ProductoInsumo> getInsumos() { return insumos; }
    public void setInsumos(List<ProductoInsumo> insumos) { this.insumos = insumos; }

    public List<ProductoComponente> getComponentes() { return componentes; }
    public void setComponentes(List<ProductoComponente> componentes) { this.componentes = componentes; }

    public List<ProductoVariante> getVariantes() { return variantes; }
    public void setVariantes(List<ProductoVariante> variantes) { this.variantes = variantes; }

    public List<ProductoGrabado> getGrabados() { return grabados; }
    public void setGrabados(List<ProductoGrabado> grabados) { this.grabados = grabados; }

    public List<ProductoFoto> getFotos() { return fotos; }
    public void setFotos(List<ProductoFoto> fotos) { this.fotos = fotos; }

    public List<AtributoFiltroValor> getAtributoValores() { return atributoValores; }
    public void setAtributoValores(List<AtributoFiltroValor> atributoValores) { this.atributoValores = atributoValores; }
}
