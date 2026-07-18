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
}
