package com.sistventas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "licencia_estado", nullable = false, length = 20)
    private LicenciaEstado licenciaEstado = LicenciaEstado.PENDIENTE;

    @Column(name = "licencia_vencimiento")
    private LocalDate licenciaVencimiento;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @Column(name = "logo_url")
    private String logoUrl;

    // Imagen de fondo de la portada del catálogo en PDF (opcional). Sin
    // ella, la portada usa el color oscuro fijo de siempre (ver
    // CatalogoServiceImpl.dibujarPortada) — no es un rediseño del catálogo
    // entero, solo la portada admite personalización.
    @Column(name = "catalogo_portada_imagen_url")
    private String catalogoPortadaImagenUrl;

    // Toggles del catálogo en PDF, configurables por el dueño desde
    // tienda-admin (ver CatalogoServiceImpl) — todos con default true salvo
    // el título personalizado (null = usa el nombre de la empresa, de
    // siempre).
    @Column(name = "catalogo_mostrar_logo", nullable = false)
    private boolean catalogoMostrarLogo = true;

    @Column(name = "catalogo_titulo_personalizado")
    private String catalogoTituloPersonalizado;

    @Column(name = "catalogo_mostrar_descripcion", nullable = false)
    private boolean catalogoMostrarDescripcion = true;

    @Column(name = "catalogo_mostrar_colores", nullable = false)
    private boolean catalogoMostrarColores = true;

    // Color de fondo de las páginas de PRODUCTOS del catálogo (hex, ej.
    // "#f7f3ec") — deliberadamente NUNCA una foto ahí (ver
    // CatalogoServiceImpl): con tanto texto/precio por página, una imagen de
    // fondo compromete la legibilidad. null = blanco de siempre.
    @Column(name = "catalogo_color_fondo_productos", length = 9)
    private String catalogoColorFondoProductos;

    // Estilo GLOBAL del texto de las páginas "Importante" y "Cómo comprar"
    // (decisión: un único estilo para toda la sección, no por bloque — ver
    // CatalogoServiceImpl.agregarBloquesDeTexto). Los 3 quedan null juntos o
    // cargados juntos: si no se personalizó nunca, caen a los defaults de
    // siempre (Helvetica 10pt gris oscuro).
    @Column(name = "catalogo_texto_fuente", length = 20)
    private String catalogoTextoFuente;

    @Column(name = "catalogo_texto_tamanio")
    private Integer catalogoTextoTamanio;

    @Column(name = "catalogo_texto_color", length = 9)
    private String catalogoTextoColor;

    @Column(length = 60, unique = true)
    private String slug;

    @Column(name = "tienda_habilitada", nullable = false)
    private boolean tiendaHabilitada = false;

    // Ambos opcionales: si no se cargan, la vidriera pública cae a un texto
    // genérico (ver PublicTiendaServiceImpl / frontend).
    @Column(name = "tienda_banner_titulo", length = 150)
    private String tiendaBannerTitulo;

    @Column(name = "tienda_banner_descripcion", length = 500)
    private String tiendaBannerDescripcion;

    @Column(name = "tienda_banner_tagline", length = 100)
    private String tiendaBannerTagline;

    // Todos opcionales, mismo criterio que los campos de banner: si no se
    // cargan, la vidriera pública no muestra esa sección de contacto (ver
    // PublicTiendaServiceImpl / frontend).
    @Column(name = "tienda_contacto_whatsapp", length = 40)
    private String tiendaContactoWhatsapp;

    @Column(name = "tienda_contacto_instagram", length = 100)
    private String tiendaContactoInstagram;

    @Column(name = "tienda_contacto_email", length = 190)
    private String tiendaContactoEmail;

    // El cupón nunca se expone en PublicEmpresaDto: se valida solo al
    // confirmar el pedido (ver crearPedido en PublicTiendaServiceImpl), así
    // que no hace falta que la vidriera lo muestre de antemano.
    @Column(name = "tienda_cupon_codigo", length = 40)
    private String tiendaCuponCodigo;

    @Column(name = "tienda_cupon_porcentaje", precision = 5, scale = 2)
    private BigDecimal tiendaCuponPorcentaje;

    // Valores válidos: "clasica" (default), "cursiva", "elegante", "moderna"
    // (ver fuentes.css en el frontend). No amerita un enum Java, mismo
    // criterio que otros campos simples de personalización de la tienda.
    @Column(name = "tienda_fuente", nullable = false, length = 20)
    private String tiendaFuente = "clasica";

    // Paleta de color de marca de la tienda pública. Valores válidos: "claro"
    // (default), "oscuro" (ver styles.css/tienda-publica.css en el frontend
    // para los tokens de cada uno). Mismo criterio que tiendaFuente: String
    // simple, no enum, la lista cerrada la controla el <select> del frontend.
    @Column(name = "tienda_tema", nullable = false, length = 20)
    private String tiendaTema = "claro";

    // Disposición de la sección "Cuidá tu mate" de la tienda pública. Valores
    // válidos: "vertical-1" (default), "vertical-2", "horizontal". Mismo
    // criterio que tiendaTema: String simple, no enum.
    @Column(name = "tienda_tips_layout", nullable = false, length = 20)
    private String tiendaTipsLayout = "vertical-1";

    // Key propia de Gemini para generar la descripción sugerida con IA
    // (Productos > lápiz sobre la foto > "Generar con IA"). Nullable a
    // propósito: sin key propia, DescripcionIaServiceImpl cae a
    // sistventas.gemini.api-key (variable de entorno global) — así el
    // sistema sigue funcionando para empresas que todavía no cargaron la
    // suya. Nunca se devuelve en texto plano por API una vez guardada (ver
    // PerfilDto.geminiApiKeyConfigurada) — solo se puede reemplazar, no leer.
    @Column(name = "gemini_api_key")
    private String geminiApiKey;

    // Dónde se renderiza la sección de banners verticales en la tienda
    // pública (ver PublicEmpresaDto.bannerVerticalPosicion /
    // tienda-publica.ts). Nullable a propósito: null = "nunca configurado",
    // el frontend público NO debe mostrar la sección aunque ya haya
    // imágenes verticales cargadas. Valores esperados: DESPUES_BANNER,
    // DESPUES_CATEGORIAS, DESPUES_DESTACADOS, ANTES_FOOTER (el frontend
    // controla el <select>, no hace falta validar la lista acá).
    @Column(name = "tienda_banner_vertical_posicion", length = 30)
    private String tiendaBannerVerticalPosicion;

    // Los 3 siguientes son opcionales, para más presencia legal en el footer
    // de la tienda pública (ver TiendaPublica.footerLegal): si no se cargan,
    // el footer simplemente omite esa línea.
    @Column(name = "tienda_razon_social", length = 150)
    private String tiendaRazonSocial;

    @Column(name = "tienda_cuit", length = 20)
    private String tiendaCuit;

    @Column(name = "tienda_direccion", length = 200)
    private String tiendaDireccion;

    // Texto libre largo (hasta ~2000 caracteres, ver ActualizarTiendaRequest)
    // mostrado en el modal "Sobre nosotros" de la tienda pública. TEXT (no
    // VARCHAR) por el mismo criterio que Venta.notas: es contenido narrativo
    // largo, no un campo corto de formulario.
    @Column(name = "tienda_sobre_nosotros", columnDefinition = "TEXT")
    private String tiendaSobreNosotros;

    // Barra de urgencia de la vidriera pública ("SOLO POR HOY..."), ahora
    // configurable por el dueño en vez de datos de prueba fijos en el
    // frontend (ver tienda-publica.ts, URGENCIA_DURACION_MS retirado). Los 4
    // campos viajan juntos: la vidriera solo muestra la barra si
    // tiendaOfertaActiva es true Y tiendaOfertaFechaFin es una fecha futura
    // (ver PublicTiendaServiceImpl) — una fecha vencida oculta la barra sola,
    // sin que el dueño tenga que desactivarla a mano.
    @Column(name = "tienda_oferta_activa", nullable = false)
    private boolean tiendaOfertaActiva = false;

    @Column(name = "tienda_oferta_etiqueta", length = 60)
    private String tiendaOfertaEtiqueta;

    @Column(name = "tienda_oferta_texto", length = 150)
    private String tiendaOfertaTexto;

    @Column(name = "tienda_oferta_fecha_fin")
    private LocalDateTime tiendaOfertaFechaFin;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public LicenciaEstado getLicenciaEstado() { return licenciaEstado; }
    public void setLicenciaEstado(LicenciaEstado licenciaEstado) { this.licenciaEstado = licenciaEstado; }

    public LocalDate getLicenciaVencimiento() { return licenciaVencimiento; }
    public void setLicenciaVencimiento(LocalDate licenciaVencimiento) { this.licenciaVencimiento = licenciaVencimiento; }

    public LocalDateTime getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDateTime fechaAlta) { this.fechaAlta = fechaAlta; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getCatalogoPortadaImagenUrl() { return catalogoPortadaImagenUrl; }
    public void setCatalogoPortadaImagenUrl(String catalogoPortadaImagenUrl) { this.catalogoPortadaImagenUrl = catalogoPortadaImagenUrl; }

    public boolean isCatalogoMostrarLogo() { return catalogoMostrarLogo; }
    public void setCatalogoMostrarLogo(boolean catalogoMostrarLogo) { this.catalogoMostrarLogo = catalogoMostrarLogo; }

    public String getCatalogoTituloPersonalizado() { return catalogoTituloPersonalizado; }
    public void setCatalogoTituloPersonalizado(String catalogoTituloPersonalizado) { this.catalogoTituloPersonalizado = catalogoTituloPersonalizado; }

    public boolean isCatalogoMostrarDescripcion() { return catalogoMostrarDescripcion; }
    public void setCatalogoMostrarDescripcion(boolean catalogoMostrarDescripcion) { this.catalogoMostrarDescripcion = catalogoMostrarDescripcion; }

    public boolean isCatalogoMostrarColores() { return catalogoMostrarColores; }
    public void setCatalogoMostrarColores(boolean catalogoMostrarColores) { this.catalogoMostrarColores = catalogoMostrarColores; }

    public String getCatalogoColorFondoProductos() { return catalogoColorFondoProductos; }
    public void setCatalogoColorFondoProductos(String catalogoColorFondoProductos) { this.catalogoColorFondoProductos = catalogoColorFondoProductos; }

    public String getCatalogoTextoFuente() { return catalogoTextoFuente; }
    public void setCatalogoTextoFuente(String catalogoTextoFuente) { this.catalogoTextoFuente = catalogoTextoFuente; }

    public Integer getCatalogoTextoTamanio() { return catalogoTextoTamanio; }
    public void setCatalogoTextoTamanio(Integer catalogoTextoTamanio) { this.catalogoTextoTamanio = catalogoTextoTamanio; }

    public String getCatalogoTextoColor() { return catalogoTextoColor; }
    public void setCatalogoTextoColor(String catalogoTextoColor) { this.catalogoTextoColor = catalogoTextoColor; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public boolean isTiendaHabilitada() { return tiendaHabilitada; }
    public void setTiendaHabilitada(boolean tiendaHabilitada) { this.tiendaHabilitada = tiendaHabilitada; }

    public String getTiendaBannerTitulo() { return tiendaBannerTitulo; }
    public void setTiendaBannerTitulo(String tiendaBannerTitulo) { this.tiendaBannerTitulo = tiendaBannerTitulo; }

    public String getTiendaBannerDescripcion() { return tiendaBannerDescripcion; }
    public void setTiendaBannerDescripcion(String tiendaBannerDescripcion) { this.tiendaBannerDescripcion = tiendaBannerDescripcion; }

    public String getTiendaBannerTagline() { return tiendaBannerTagline; }
    public void setTiendaBannerTagline(String tiendaBannerTagline) { this.tiendaBannerTagline = tiendaBannerTagline; }

    public String getTiendaContactoWhatsapp() { return tiendaContactoWhatsapp; }
    public void setTiendaContactoWhatsapp(String tiendaContactoWhatsapp) { this.tiendaContactoWhatsapp = tiendaContactoWhatsapp; }

    public String getTiendaContactoInstagram() { return tiendaContactoInstagram; }
    public void setTiendaContactoInstagram(String tiendaContactoInstagram) { this.tiendaContactoInstagram = tiendaContactoInstagram; }

    public String getTiendaContactoEmail() { return tiendaContactoEmail; }
    public void setTiendaContactoEmail(String tiendaContactoEmail) { this.tiendaContactoEmail = tiendaContactoEmail; }

    public String getTiendaCuponCodigo() { return tiendaCuponCodigo; }
    public void setTiendaCuponCodigo(String tiendaCuponCodigo) { this.tiendaCuponCodigo = tiendaCuponCodigo; }

    public BigDecimal getTiendaCuponPorcentaje() { return tiendaCuponPorcentaje; }
    public void setTiendaCuponPorcentaje(BigDecimal tiendaCuponPorcentaje) { this.tiendaCuponPorcentaje = tiendaCuponPorcentaje; }

    public String getTiendaFuente() { return tiendaFuente; }
    public void setTiendaFuente(String tiendaFuente) { this.tiendaFuente = tiendaFuente; }

    public String getTiendaTema() { return tiendaTema; }
    public void setTiendaTema(String tiendaTema) { this.tiendaTema = tiendaTema; }
    public String getTiendaTipsLayout() { return tiendaTipsLayout; }
    public void setTiendaTipsLayout(String tiendaTipsLayout) { this.tiendaTipsLayout = tiendaTipsLayout; }

    public String getGeminiApiKey() { return geminiApiKey; }
    public void setGeminiApiKey(String geminiApiKey) { this.geminiApiKey = geminiApiKey; }

    public String getTiendaBannerVerticalPosicion() { return tiendaBannerVerticalPosicion; }
    public void setTiendaBannerVerticalPosicion(String tiendaBannerVerticalPosicion) { this.tiendaBannerVerticalPosicion = tiendaBannerVerticalPosicion; }

    public String getTiendaRazonSocial() { return tiendaRazonSocial; }
    public void setTiendaRazonSocial(String tiendaRazonSocial) { this.tiendaRazonSocial = tiendaRazonSocial; }

    public String getTiendaCuit() { return tiendaCuit; }
    public void setTiendaCuit(String tiendaCuit) { this.tiendaCuit = tiendaCuit; }

    public String getTiendaDireccion() { return tiendaDireccion; }
    public void setTiendaDireccion(String tiendaDireccion) { this.tiendaDireccion = tiendaDireccion; }

    public String getTiendaSobreNosotros() { return tiendaSobreNosotros; }
    public void setTiendaSobreNosotros(String tiendaSobreNosotros) { this.tiendaSobreNosotros = tiendaSobreNosotros; }

    public boolean isTiendaOfertaActiva() { return tiendaOfertaActiva; }
    public void setTiendaOfertaActiva(boolean tiendaOfertaActiva) { this.tiendaOfertaActiva = tiendaOfertaActiva; }

    public String getTiendaOfertaEtiqueta() { return tiendaOfertaEtiqueta; }
    public void setTiendaOfertaEtiqueta(String tiendaOfertaEtiqueta) { this.tiendaOfertaEtiqueta = tiendaOfertaEtiqueta; }

    public String getTiendaOfertaTexto() { return tiendaOfertaTexto; }
    public void setTiendaOfertaTexto(String tiendaOfertaTexto) { this.tiendaOfertaTexto = tiendaOfertaTexto; }

    public LocalDateTime getTiendaOfertaFechaFin() { return tiendaOfertaFechaFin; }
    public void setTiendaOfertaFechaFin(LocalDateTime tiendaOfertaFechaFin) { this.tiendaOfertaFechaFin = tiendaOfertaFechaFin; }
}
