package com.sistventas.backend.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.sistventas.backend.entity.Empresa;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ProductoFoto;
import com.sistventas.backend.entity.ProductoVariante;
import com.sistventas.backend.entity.TiendaCatalogoSeccion;
import com.sistventas.backend.exception.EmpresaNoEncontradaException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.EmpresaRepository;
import com.sistventas.backend.repository.ProductoRepository;
import com.sistventas.backend.repository.TiendaCatalogoSeccionRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.CatalogoService;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Exportación del catálogo de productos activos de la empresa a PDF y Excel.
 * Mismo scoping multiempresa que ProductoServiceImpl: el empresaId siempre
 * sale del UserPrincipal, nunca de un valor del cliente.
 */
@Service
public class CatalogoServiceImpl implements CatalogoService {

    // Mismo Path base que ProductoServiceImpl usa para guardar las fotos: la
    // URL persistida (/uploads/productos/{archivo}) solo tiene sentido
    // resuelta contra este directorio local.
    private static final Path UPLOAD_DIR = Paths.get("uploads", "productos");

    // Distinta carpeta que las fotos de producto: la imagen de portada del
    // catálogo se sube junto con el logo (ver PerfilServiceImpl.
    // actualizarCatalogoPortadaImagen), que guarda en uploads/empresas/.
    private static final Path UPLOAD_DIR_EMPRESAS = Paths.get("uploads", "empresas");

    private static final float LOGO_PORTADA_ALTO = 70f;

    // Paleta fija del diseño del catálogo (decisión: diseño programado, no
    // imagen de fondo subida por la empresa — ver plan de la feature). Un
    // solo estilo oscuro/prolijo para portada y bandas de encabezado, igual
    // para todas las empresas.
    private static final Color COLOR_OSCURO = new Color(26, 26, 26);
    private static final Color COLOR_GRIS_TEXTO = new Color(90, 90, 90);

    private static final float ALTO_BANDA_ENCABEZADO = 46f;

    private final ProductoRepository productoRepository;
    private final EmpresaRepository empresaRepository;
    private final ProductoFotoResolver productoFotoResolver;
    private final TiendaCatalogoSeccionRepository tiendaCatalogoSeccionRepository;

    public CatalogoServiceImpl(ProductoRepository productoRepository, EmpresaRepository empresaRepository,
                                ProductoFotoResolver productoFotoResolver,
                                TiendaCatalogoSeccionRepository tiendaCatalogoSeccionRepository) {
        this.productoRepository = productoRepository;
        this.empresaRepository = empresaRepository;
        this.productoFotoResolver = productoFotoResolver;
        this.tiendaCatalogoSeccionRepository = tiendaCatalogoSeccionRepository;
    }

    // Miniatura de catálogo (PDF/Excel): resuelta vía ProductoFotoResolver
    // sobre el pool, NUNCA producto.getFotoUrl() directo (esa columna está
    // deprecada y puede quedar desactualizada apenas alguien reordene o
    // borre fotos desde el admin — ver design "silent-break risk" en
    // CatalogoServiceImpl).
    private String miniaturaDe(Producto producto) {
        return productoFotoResolver.resolverMiniatura(producto.getFotos().stream()
                .sorted(java.util.Comparator.comparingInt(ProductoFoto::getOrden))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarPdf(UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(EmpresaNoEncontradaException::new);
        List<Producto> productos = productosOrdenados(empresaId);

        // Tipografía/tamaño/color de "Importante"/"Cómo comprar": estilo
        // GLOBAL elegido por el dueño (ver Empresa.catalogoTextoFuente y
        // similares) — null en cualquiera de los 3 cae al default de
        // siempre (Helvetica 10pt gris oscuro).
        int fuenteBaseTexto = fuenteBasePorId(empresa.getCatalogoTextoFuente());
        int tamanioTexto = empresa.getCatalogoTextoTamanio() != null ? empresa.getCatalogoTextoTamanio() : 10;
        Color colorTexto = parsearColorHex(empresa.getCatalogoTextoColor());
        // Sin personalizar (los 3 en null): mismo comportamiento de siempre,
        // negro plano — nunca se fuerza un color por default.
        Font cuerpoFont = colorTexto != null
                ? new Font(fuenteBaseTexto, tamanioTexto, Font.NORMAL, colorTexto)
                : new Font(fuenteBaseTexto, tamanioTexto, Font.NORMAL);
        Font categoriaFont = new Font(Font.HELVETICA, 14, Font.BOLD, Color.WHITE);
        Font subcategoriaFont = new Font(Font.HELVETICA, 11, Font.BOLDITALIC, COLOR_GRIS_TEXTO);
        Font nombreFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font colorFont = new Font(Font.HELVETICA, 9, Font.ITALIC, COLOR_GRIS_TEXTO);
        Font precioFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font vacioFont = new Font(Font.HELVETICA, 11, Font.ITALIC);

        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            FondoPaginaProductosEvent fondoPaginaEvent = new FondoPaginaProductosEvent(
                    parsearColorHex(empresa.getCatalogoColorFondoProductos()));
            writer.setPageEvent(fondoPaginaEvent);
            document.open();

            String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String tituloPortada = empresa.getCatalogoTituloPersonalizado() != null
                    ? empresa.getCatalogoTituloPersonalizado()
                    : empresa.getNombre();
            String logoPortadaUrl = empresa.isCatalogoMostrarLogo() ? empresa.getLogoUrl() : null;
            dibujarPortada(writer, tituloPortada, fecha, empresa.getCatalogoPortadaImagenUrl(), logoPortadaUrl);

            List<TiendaCatalogoSeccion> puntosImportante = tiendaCatalogoSeccionRepository
                    .findByEmpresaIdAndTipoOrderByOrdenAscIdAsc(empresaId, "IMPORTANTE");
            if (!puntosImportante.isEmpty()) {
                document.newPage();
                dibujarBandaEncabezado(writer, "IMPORTANTE");
                agregarBloquesDeTexto(document, puntosImportante, cuerpoFont);
            }

            List<TiendaCatalogoSeccion> puntosComoComprar = tiendaCatalogoSeccionRepository
                    .findByEmpresaIdAndTipoOrderByOrdenAscIdAsc(empresaId, "COMO_COMPRAR");
            boolean tieneComoComprar = !puntosComoComprar.isEmpty();
            boolean tieneContacto = tieneAlgunContacto(empresa);
            if (tieneComoComprar || tieneContacto) {
                document.newPage();
                dibujarBandaEncabezado(writer, "CÓMO COMPRAR");
                if (tieneComoComprar) {
                    agregarBloquesDeTexto(document, puntosComoComprar, cuerpoFont);
                }
                if (tieneContacto) {
                    Paragraph contactoTitulo = new Paragraph("Contacto", new Font(Font.HELVETICA, 11, Font.BOLD));
                    contactoTitulo.setSpacingBefore(14f);
                    document.add(contactoTitulo);
                    if (empresa.getTiendaContactoWhatsapp() != null) {
                        document.add(new Paragraph("WhatsApp: " + empresa.getTiendaContactoWhatsapp(), cuerpoFont));
                    }
                    if (empresa.getTiendaContactoInstagram() != null) {
                        document.add(new Paragraph("Instagram: " + empresa.getTiendaContactoInstagram(), cuerpoFont));
                    }
                    if (empresa.getTiendaContactoEmail() != null) {
                        document.add(new Paragraph("Email: " + empresa.getTiendaContactoEmail(), cuerpoFont));
                    }
                }
            }

            // A partir de acá (portada/Importante/Cómo comprar ya quedaron
            // atrás — sin página de Índice, sacada a pedido) es donde el
            // color de fondo de productos entra
            // en juego — se activa recién ahora, nunca en las páginas fijas
            // de arriba (ver plan: "el fondo para los productos es una cosa,
            // para todo lo demás es otro").
            fondoPaginaEvent.activo = true;
            document.newPage();
            if (productos.isEmpty()) {
                document.add(new Paragraph("Todavía no hay productos cargados.", vacioFont));
            } else {
                String categoriaActual = null;
                String subcategoriaActual = null;
                List<Producto> grupoActual = new ArrayList<>();
                for (Producto producto : productos) {
                    String nombreCategoria = producto.getCategoria().getNombre();
                    String nombreSubcategoria = producto.getSubcategoria() != null ? producto.getSubcategoria().getNombre() : null;
                    boolean cambioCategoria = !nombreCategoria.equals(categoriaActual);
                    boolean cambioSubcategoria = cambioCategoria || !java.util.Objects.equals(nombreSubcategoria, subcategoriaActual);

                    if (cambioSubcategoria) {
                        if (!grupoActual.isEmpty()) {
                            agregarFilasProductos(document, grupoActual, empresa, nombreFont, colorFont, precioFont);
                            grupoActual = new ArrayList<>();
                        }
                        if (cambioCategoria) {
                            categoriaActual = nombreCategoria;
                            agregarBarraCategoria(document, categoriaActual, categoriaFont);
                        }
                        subcategoriaActual = nombreSubcategoria;
                        if (subcategoriaActual != null) {
                            Paragraph subcategoriaTitulo = new Paragraph(subcategoriaActual, subcategoriaFont);
                            subcategoriaTitulo.setSpacingBefore(8f);
                            subcategoriaTitulo.setSpacingAfter(4f);
                            document.add(subcategoriaTitulo);
                        }
                    }
                    grupoActual.add(producto);
                }
                if (!grupoActual.isEmpty()) {
                    agregarFilasProductos(document, grupoActual, empresa, nombreFont, colorFont, precioFont);
                }
            }

            document.close();
        } catch (DocumentException | java.io.IOException ex) {
            throw new IllegalStateException("No se pudo generar el catálogo en PDF", ex);
        }

        return baos.toByteArray();
    }

    // Cada TiendaCatalogoSeccion se muestra TAL CUAL la escribió el admin —
    // sin numerar ni reformatear — respetando sus saltos de línea. OpenPDF no
    // interpreta "\n" dentro de un mismo Paragraph como salto de línea, así
    // que se parte el texto por línea y se agrega un Paragraph por cada una
    // (spacing 0 entre líneas del mismo bloque, spacing mayor recién después
    // de la última línea, separando un bloque del siguiente).
    private void agregarBloquesDeTexto(Document document, List<TiendaCatalogoSeccion> bloques, Font font) throws DocumentException {
        for (TiendaCatalogoSeccion bloque : bloques) {
            Font fontBloque = fontConEstiloDeBloque(bloque, font);
            int alineacion = alineacionOpenPdf(bloque.getAlineacion());
            String[] lineas = bloque.getTexto().split("\n", -1);
            for (int i = 0; i < lineas.length; i++) {
                Paragraph parrafo = new Paragraph(lineas[i], fontBloque);
                parrafo.setAlignment(alineacion);
                parrafo.setSpacingAfter(i == lineas.length - 1 ? 12f : 0f);
                document.add(parrafo);
            }
        }
    }

    // Estilo POR BLOQUE (V58: negrita/cursiva/subrayado) ENCIMA de la fuente
    // GLOBAL (V57: familia/tamaño/color, la misma para todos los puntos) —
    // se combinan los bits de Font.BOLD/ITALIC/UNDERLINE sobre la familia y
    // tamaño ya elegidos, sin tocar el color (que sigue siendo el global).
    private Font fontConEstiloDeBloque(TiendaCatalogoSeccion bloque, Font fontBase) {
        int estilo = Font.NORMAL;
        if (bloque.isNegrita()) {
            estilo |= Font.BOLD;
        }
        if (bloque.isCursiva()) {
            estilo |= Font.ITALIC;
        }
        if (bloque.isSubrayado()) {
            estilo |= Font.UNDERLINE;
        }
        return new Font(fontBase.getFamily(), fontBase.getSize(), estilo, fontBase.getColor());
    }

    private int alineacionOpenPdf(String alineacion) {
        return switch (alineacion) {
            case "CENTRO" -> Element.ALIGN_CENTER;
            case "DERECHA" -> Element.ALIGN_RIGHT;
            default -> Element.ALIGN_LEFT;
        };
    }

    private boolean tieneAlgunContacto(Empresa empresa) {
        return empresa.getTiendaContactoWhatsapp() != null
                || empresa.getTiendaContactoInstagram() != null
                || empresa.getTiendaContactoEmail() != null;
    }

    // Portada de página completa: si la empresa subió una imagen de fondo
    // (ver Empresa.catalogoPortadaImagenUrl), se dibuja recortada tipo
    // "cover" (llena la página entera, sin deformar ni dejar franjas) con un
    // overlay oscuro semi-transparente encima para que el título en blanco
    // siga siendo legible sin importar la foto. Sin imagen, se mantiene el
    // rectángulo oscuro sólido de siempre (comportamiento original).
    private void dibujarPortada(PdfWriter writer, String nombreEmpresa, String fecha, String portadaImagenUrl, String logoUrl)
            throws DocumentException, java.io.IOException {
        PdfContentByte cb = writer.getDirectContent();
        float ancho = PageSize.A4.getWidth();
        float alto = PageSize.A4.getHeight();

        Image imagenFondo = cargarImagenPortada(portadaImagenUrl);
        if (imagenFondo != null) {
            float escala = Math.max(ancho / imagenFondo.getWidth(), alto / imagenFondo.getHeight());
            float anchoEscalado = imagenFondo.getWidth() * escala;
            float altoEscalado = imagenFondo.getHeight() * escala;
            float offsetX = (ancho - anchoEscalado) / 2f;
            float offsetY = (alto - altoEscalado) / 2f;
            cb.addImage(imagenFondo, anchoEscalado, 0, 0, altoEscalado, offsetX, offsetY);

            cb.saveState();
            PdfGState estadoTransparente = new PdfGState();
            // 0.65 en vez de 0.55: algunas fotos de fondo traen su propio
            // texto/logo de marca superpuesto (marca de agua), y con el
            // overlay más liviano competía visualmente con el título.
            estadoTransparente.setFillOpacity(0.65f);
            cb.setGState(estadoTransparente);
            cb.setColorFill(COLOR_OSCURO);
            cb.rectangle(0, 0, ancho, alto);
            cb.fill();
            cb.restoreState();
        } else {
            cb.setColorFill(COLOR_OSCURO);
            cb.rectangle(0, 0, ancho, alto);
            cb.fill();
        }

        Image logo = cargarImagenPortada(logoUrl);
        if (logo != null) {
            float escalaLogo = LOGO_PORTADA_ALTO / logo.getHeight();
            float anchoLogo = logo.getWidth() * escalaLogo;
            cb.addImage(logo, anchoLogo, 0, 0, LOGO_PORTADA_ALTO, (ancho - anchoLogo) / 2f, alto / 2f + 50);
        }

        BaseFont tituloBase = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        BaseFont normalBase = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);

        // Sombra: una copia del texto en negro, corrida 1.2pt, debajo del
        // texto real — garantiza contraste legible sin importar qué tan
        // "ruidosa" sea la foto de fondo (útil sobre todo cuando esa foto
        // ya trae su propio texto/logo superpuesto).
        cb.beginText();
        cb.setColorFill(COLOR_OSCURO);
        cb.setFontAndSize(tituloBase, 28);
        cb.showTextAligned(Element.ALIGN_CENTER, nombreEmpresa, ancho / 2f + 1.2f, alto / 2f + 10.8f, 0);
        cb.setFontAndSize(normalBase, 11);
        cb.showTextAligned(Element.ALIGN_CENTER, "Catálogo generado el " + fecha, ancho / 2f + 1.2f, alto / 2f - 13.2f, 0);
        cb.endText();

        cb.beginText();
        cb.setColorFill(Color.WHITE);
        cb.setFontAndSize(tituloBase, 28);
        cb.showTextAligned(Element.ALIGN_CENTER, nombreEmpresa, ancho / 2f, alto / 2f + 12, 0);
        cb.setColorFill(new Color(200, 200, 200));
        cb.setFontAndSize(normalBase, 11);
        cb.showTextAligned(Element.ALIGN_CENTER, "Catálogo generado el " + fecha, ancho / 2f, alto / 2f - 12, 0);
        cb.endText();
    }

    // Banda oscura fija en el margen superior de la página actual + título
    // en blanco: mismo estilo en las 3 páginas de contenido fijo (Importante,
    // Cómo comprar, Índice). Se dibuja en el área del margen, así el flujo
    // normal de document.add() que arranca justo debajo no se ve afectado.
    private void dibujarBandaEncabezado(PdfWriter writer, String titulo) throws DocumentException, java.io.IOException {
        PdfContentByte cb = writer.getDirectContent();
        float ancho = PageSize.A4.getWidth();
        float alto = PageSize.A4.getHeight();

        cb.setColorFill(COLOR_OSCURO);
        cb.rectangle(0, alto - ALTO_BANDA_ENCABEZADO, ancho, ALTO_BANDA_ENCABEZADO);
        cb.fill();

        BaseFont tituloBase = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        cb.beginText();
        cb.setColorFill(Color.WHITE);
        cb.setFontAndSize(tituloBase, 16);
        cb.showTextAligned(Element.ALIGN_LEFT, titulo, 36, alto - ALTO_BANDA_ENCABEZADO / 2f - 6, 0);
        cb.endText();
    }

    // Título de categoría dentro de una barra oscura de ancho completo
    // (PdfPTable de 1 celda con fondo), igual que dibujarBandaEncabezado pero
    // integrado al flujo normal del documento (no en el margen: se repite
    // varias veces por página a medida que cambia la categoría).
    private void agregarBarraCategoria(Document document, String nombreCategoria, Font categoriaFont) throws DocumentException {
        PdfPTable barra = new PdfPTable(1);
        barra.setWidthPercentage(100);
        barra.setSpacingBefore(14f);
        barra.setSpacingAfter(8f);

        PdfPCell celda = new PdfPCell(new Paragraph(nombreCategoria, categoriaFont));
        celda.setBackgroundColor(COLOR_OSCURO);
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPadding(8f);
        barra.addCell(celda);

        document.add(barra);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarExcel(UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        List<Producto> productos = productosOrdenados(empresaId);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Catálogo");

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle categoriaStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font categoriaFont = workbook.createFont();
            categoriaFont.setBold(true);
            categoriaFont.setFontHeightInPoints((short) 13);
            categoriaStyle.setFont(categoriaFont);

            String[] encabezados = {"Foto", "Nombre", "Descripción", "Precio"};
            Row filaEncabezado = sheet.createRow(0);
            for (int i = 0; i < encabezados.length; i++) {
                var celda = filaEncabezado.createCell(i);
                celda.setCellValue(encabezados[i]);
                celda.setCellStyle(headerStyle);
            }
            sheet.setColumnWidth(0, 3200);

            Drawing<?> drawing = sheet.createDrawingPatriarch();

            int numeroFila = 1;
            String categoriaActual = null;
            for (Producto producto : productos) {
                String nombreCategoria = producto.getCategoria().getNombre();
                if (!nombreCategoria.equals(categoriaActual)) {
                    categoriaActual = nombreCategoria;
                    Row filaCategoria = sheet.createRow(numeroFila++);
                    var celdaCategoria = filaCategoria.createCell(0);
                    celdaCategoria.setCellValue(categoriaActual);
                    celdaCategoria.setCellStyle(categoriaStyle);
                }

                Row fila = sheet.createRow(numeroFila);
                fila.setHeightInPoints(60f);
                fila.createCell(1).setCellValue(producto.getNombre());
                fila.createCell(2).setCellValue(producto.getDescripcion() != null ? producto.getDescripcion() : "");
                fila.createCell(3).setCellValue(producto.getPrecioVenta().doubleValue());

                insertarImagen(workbook, drawing, miniaturaDe(producto), numeroFila);
                numeroFila++;
            }

            for (int i = 1; i < encabezados.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (java.io.IOException ex) {
            throw new UncheckedIOException("No se pudo generar el catálogo en Excel", ex);
        }
    }

    // La imagen vive en disco local, fuera de la transacción de BD. Si no
    // existe o el formato no se reconoce, se omite en silencio — no debe
    // romper la generación del Excel entero por una foto puntual.
    private void insertarImagen(Workbook workbook, Drawing<?> drawing, String fotoUrl, int numeroFila) {
        if (fotoUrl == null || fotoUrl.isBlank()) {
            return;
        }
        try {
            String nombreArchivo = fotoUrl.substring(fotoUrl.lastIndexOf('/') + 1);
            Path rutaImagen = UPLOAD_DIR.resolve(nombreArchivo);
            int tipoImagen = tipoImagenPoi(nombreArchivo);
            if (tipoImagen == -1 || !Files.exists(rutaImagen)) {
                return;
            }

            byte[] bytes = Files.readAllBytes(rutaImagen);
            int indiceImagen = workbook.addPicture(bytes, tipoImagen);

            ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, numeroFila, 1, numeroFila + 1);
            drawing.createPicture(anchor, indiceImagen);
        } catch (Exception ex) {
            // Imagen no cargable: se omite, no interrumpe la generación del Excel.
        }
    }

    private int tipoImagenPoi(String nombreArchivo) {
        String lower = nombreArchivo.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return Workbook.PICTURE_TYPE_PNG;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return Workbook.PICTURE_TYPE_JPEG;
        }
        return -1;
    }

    // Fila horizontal por producto (reemplaza la vieja grilla de 3 columnas
    // con tarjetas verticales — pedido explícito: "en una fila, el título,
    // colores, foto y precio"). Sin descripción a propósito ("quiero que se
    // vea el título nomás") — sigue viva en la tienda online,
    // PublicProductoDto nunca dejó de mandarla, esto es solo el impreso.
    // Cuando el producto tiene variantes de color, se muestra UNA foto por
    // color (ver urlsFotosPorColor) en vez de una sola miniatura genérica.
    private static final float[] ANCHOS_COLUMNAS_FILA = { 2.4f, 2.2f, 3.2f, 1.4f };
    private static final float FOTO_FILA_SIZE = 55f;

    private void agregarFilasProductos(Document document, List<Producto> productos, Empresa empresa,
            Font nombreFont, Font colorFont, Font precioFont) throws DocumentException {
        for (Producto producto : productos) {
            document.add(filaProducto(producto, empresa, nombreFont, colorFont, precioFont));
        }
    }

    private PdfPTable filaProducto(Producto producto, Empresa empresa, Font nombreFont, Font colorFont, Font precioFont)
            throws DocumentException {
        PdfPTable fila = new PdfPTable(ANCHOS_COLUMNAS_FILA.length);
        fila.setWidths(ANCHOS_COLUMNAS_FILA);
        fila.setWidthPercentage(100);
        fila.setSpacingAfter(4f);

        PdfPCell celdaNombre = new PdfPCell(new Paragraph(producto.getNombre(), nombreFont));
        celdaNombre.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celdaNombre.setBorder(Rectangle.BOTTOM);
        celdaNombre.setBorderColor(COLOR_GRIS_TEXTO);
        celdaNombre.setPaddingTop(6f);
        celdaNombre.setPaddingBottom(6f);
        celdaNombre.setPaddingLeft(4f);
        fila.addCell(celdaNombre);

        String colores = (empresa.isCatalogoMostrarColores() && !producto.getVariantes().isEmpty())
                ? producto.getVariantes().stream()
                        .map(ProductoVariante::getColor)
                        .collect(java.util.stream.Collectors.joining(" · "))
                : "";
        PdfPCell celdaColores = new PdfPCell(new Paragraph(colores, colorFont));
        celdaColores.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celdaColores.setBorder(Rectangle.BOTTOM);
        celdaColores.setBorderColor(COLOR_GRIS_TEXTO);
        celdaColores.setPaddingTop(6f);
        celdaColores.setPaddingBottom(6f);
        fila.addCell(celdaColores);

        fila.addCell(celdaFotosPorColor(producto));

        PdfPCell celdaPrecio = new PdfPCell(new Paragraph(formatPrecio(producto.getPrecioVenta()), precioFont));
        celdaPrecio.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celdaPrecio.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaPrecio.setBorder(Rectangle.BOTTOM);
        celdaPrecio.setBorderColor(COLOR_GRIS_TEXTO);
        celdaPrecio.setPaddingTop(6f);
        celdaPrecio.setPaddingBottom(6f);
        celdaPrecio.setPaddingRight(4f);
        fila.addCell(celdaPrecio);

        return fila;
    }

    // Una foto por color (o la única miniatura genérica si el producto no
    // tiene variantes) puestas en línea vía una mini-tabla anidada — es la
    // forma de que varias Image queden una al lado de la otra DENTRO de una
    // sola celda de la tabla externa (OpenPDF apila los elementos de una
    // celda verticalmente por defecto).
    private PdfPCell celdaFotosPorColor(Producto producto) throws DocumentException {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(COLOR_GRIS_TEXTO);
        celda.setPadding(4f);

        List<String> urls = urlsFotosPorColor(producto);
        if (urls.isEmpty()) {
            return celda;
        }
        PdfPTable fotos = new PdfPTable(urls.size());
        fotos.setWidthPercentage(100);
        for (String url : urls) {
            PdfPCell celdaFoto = new PdfPCell();
            celdaFoto.setBorder(Rectangle.NO_BORDER);
            celdaFoto.setHorizontalAlignment(Element.ALIGN_CENTER);
            // Alto FIJO (no solo el tamaño de la imagen): sin esto, una foto
            // apaisada (más ancha que alta) queda con menos alto real que
            // una vertical, y las celdas de la fila terminan de alturas
            // distintas — "pegadas arriba" en vez de centradas. Con alto
            // fijo + vertical middle, todas las fotos de la fila quedan
            // alineadas al medio sin importar su relación de aspecto
            // original (no se recorta el contenido, solo se centra dentro
            // del marco).
            celdaFoto.setFixedHeight(FOTO_FILA_SIZE + 8f);
            celdaFoto.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Image imagen = cargarImagen(url);
            if (imagen != null) {
                imagen.scaleToFit(FOTO_FILA_SIZE, FOTO_FILA_SIZE);
                imagen.setAlignment(Image.ALIGN_CENTER);
                celdaFoto.addElement(imagen);
            }
            fotos.addCell(celdaFoto);
        }
        celda.addElement(fotos);
        return celda;
    }

    // Sin variantes: la única miniatura genérica del producto (o ninguna).
    // Con variantes: una foto por color — la propia de esa variante si
    // existe (ver ProductoFotoResolver.fotosDeVariante), si no la miniatura
    // genérica como respaldo (nunca un casillero vacío por elección de
    // diseño, mejor repetir la foto general que no mostrar nada de ese
    // color).
    private List<String> urlsFotosPorColor(Producto producto) {
        String miniaturaGenerica = miniaturaDe(producto);
        if (producto.getVariantes().isEmpty()) {
            return miniaturaGenerica != null ? List.of(miniaturaGenerica) : List.of();
        }
        List<ProductoFoto> fotos = producto.getFotos();
        return producto.getVariantes().stream()
                // variante.getId() == null: variante recién creada, todavía
                // sin persistir (no debería llegar así a generarPdf en la
                // práctica, pero ProductoFotoResolver.fotosDeVariante no
                // tolera un varianteId null — cae directo a la miniatura
                // genérica en vez de arriesgar un NPE acá.
                .map(variante -> variante.getId() == null
                        ? miniaturaGenerica
                        : productoFotoResolver.fotosDeVariante(fotos, variante.getId()).stream()
                                .findFirst()
                                .map(ProductoFoto::getUrl)
                                .orElse(miniaturaGenerica))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    // La imagen vive en disco local, fuera de la transacción de BD. Si el
    // archivo no existe o falla al cargarse (corrupto, permisos, etc.) no
    // debe romper la generación del PDF entero: se omite solo esa miniatura.
    // Sin scaleToFit acá adentro a propósito: cada llamador escala al tamaño
    // que necesita (antes solo había un tamaño de miniatura, ahora la fila
    // por color usa uno más chico que, por ejemplo, la portada).
    private Image cargarImagen(String fotoUrl) {
        if (fotoUrl == null || fotoUrl.isBlank()) {
            return null;
        }
        try {
            String nombreArchivo = fotoUrl.substring(fotoUrl.lastIndexOf('/') + 1);
            Path rutaImagen = UPLOAD_DIR.resolve(nombreArchivo);
            if (Files.exists(rutaImagen)) {
                Image imagen = Image.getInstance(rutaImagen.toString());
                aplicarRotacionExif(imagen, rutaImagen);
                return imagen;
            }
        } catch (Exception ex) {
            // Imagen no cargable: seguimos sin ella, no interrumpimos el PDF.
        }
        return null;
    }

    // Igual criterio best-effort que cargarImagen: si el archivo no existe o
    // falla al cargarse, la portada cae al color sólido de siempre en vez de
    // romper la generación del PDF entero.
    private Image cargarImagenPortada(String portadaImagenUrl) {
        if (portadaImagenUrl == null || portadaImagenUrl.isBlank()) {
            return null;
        }
        try {
            String nombreArchivo = portadaImagenUrl.substring(portadaImagenUrl.lastIndexOf('/') + 1);
            Path ruta = UPLOAD_DIR_EMPRESAS.resolve(nombreArchivo);
            if (Files.exists(ruta)) {
                Image imagen = Image.getInstance(ruta.toString());
                aplicarRotacionExif(imagen, ruta);
                return imagen;
            }
        } catch (Exception ex) {
            // Imagen no cargable: la portada cae al color sólido, no
            // interrumpimos el PDF.
        }
        return null;
    }

    // OpenPDF (com.lowagie.text.Image) embebe los píxeles crudos del
    // archivo tal cual, ignorando el tag EXIF Orientation que la mayoría de
    // cámaras/celulares graban en fotos verticales — el resultado es la
    // foto "acostada" en el PDF aunque se vea derecha en cualquier otro
    // visor (que sí respeta ese tag). setRotationDegrees es lo que OpenPDF
    // SÍ aplica al dibujar la imagen en la página, así que corrige el
    // problema sin tener que re-rotar los píxeles a mano.
    private void aplicarRotacionExif(Image imagen, Path rutaArchivo) {
        int orientacion = ExifOrientationReader.leer(rutaArchivo);
        float grados = switch (orientacion) {
            case 3 -> 180f;
            case 6 -> 270f;
            case 8 -> 90f;
            default -> 0f;
        };
        if (grados != 0f) {
            imagen.setRotationDegrees(grados);
        }
    }

    // Color de fondo elegido por el dueño para las páginas de PRODUCTOS
    // (nunca portada/Importante/Cómo comprar/Índice, ver `activo`). null =
    // sin personalizar, no pinta nada (fondo blanco de siempre).
    private static final class FondoPaginaProductosEvent extends PdfPageEventHelper {
        private final Color color;
        private boolean activo = false;

        private FondoPaginaProductosEvent(Color color) {
            this.color = color;
        }

        @Override
        public void onStartPage(PdfWriter writer, Document document) {
            if (!activo || color == null) {
                return;
            }
            PdfContentByte cb = writer.getDirectContentUnder();
            cb.setColorFill(color);
            cb.rectangle(0, 0, document.getPageSize().getWidth(), document.getPageSize().getHeight());
            cb.fill();
        }
    }

    // Formato validado en ActualizarCatalogoConfigRequest ("#rrggbb"), pero
    // si igual llegara algo raro (dato viejo, edición manual en BD), se
    // ignora en vez de romper la generación del PDF entero.
    private Color parsearColorHex(String hex) {
        if (hex == null || hex.isBlank()) {
            return null;
        }
        try {
            return Color.decode(hex);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // Solo los 3 tipos base que trae OpenPDF sin necesidad de embeber una
    // fuente (ver Empresa.catalogoTextoFuente) — cualquier valor no
    // reconocido cae a Helvetica, el default de siempre.
    private int fuenteBasePorId(String fuenteId) {
        if ("times".equals(fuenteId)) {
            return Font.TIMES_ROMAN;
        }
        if ("courier".equals(fuenteId)) {
            return Font.COURIER;
        }
        return Font.HELVETICA;
    }

    private List<Producto> productosOrdenados(Long empresaId) {
        return productoRepository.findByEmpresaIdAndActivoTrue(empresaId).stream()
                .sorted(Comparator.<Producto, String>comparing(p -> p.getCategoria().getNombre(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(p -> p.getSubcategoria() != null ? p.getSubcategoria().getNombre() : "", String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Producto::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    // NumberFormat no es thread-safe: se instancia por llamada, mismo patrón
    // que VentaServiceImpl.formatMonto para el texto de WhatsApp.
    private String formatPrecio(BigDecimal precio) {
        NumberFormat formatter = NumberFormat.getIntegerInstance(new Locale("es", "AR"));
        return "$" + formatter.format(precio.setScale(0, RoundingMode.HALF_UP));
    }

    private Long empresaIdOrThrow(UserPrincipal principal) {
        if (principal == null || principal.empresaId() == null) {
            throw new SinEmpresaException();
        }
        return principal.empresaId();
    }
}
