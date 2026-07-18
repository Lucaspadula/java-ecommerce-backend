package com.sistventas.backend.service.impl;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sistventas.backend.entity.Empresa;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.exception.EmpresaNoEncontradaException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.EmpresaRepository;
import com.sistventas.backend.repository.ProductoRepository;
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

    private static final float THUMBNAIL_SIZE = 80f;

    private final ProductoRepository productoRepository;
    private final EmpresaRepository empresaRepository;

    public CatalogoServiceImpl(ProductoRepository productoRepository, EmpresaRepository empresaRepository) {
        this.productoRepository = productoRepository;
        this.empresaRepository = empresaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarPdf(UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(EmpresaNoEncontradaException::new);
        List<Producto> productos = productosOrdenados(empresaId);

        Font tituloFont = new Font(Font.HELVETICA, 20, Font.BOLD);
        Font fechaFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font categoriaFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font nombreFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font descripcionFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
        Font precioFont = new Font(Font.HELVETICA, 11, Font.BOLD);
        Font vacioFont = new Font(Font.HELVETICA, 11, Font.ITALIC);

        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            document.add(new Paragraph(empresa.getNombre(), tituloFont));
            String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            document.add(new Paragraph("Catálogo generado el " + fecha, fechaFont));
            document.add(Chunk.NEWLINE);

            if (productos.isEmpty()) {
                document.add(new Paragraph("Todavía no hay productos cargados.", vacioFont));
            } else {
                String categoriaActual = null;
                for (Producto producto : productos) {
                    String nombreCategoria = producto.getCategoria().getNombre();
                    if (!nombreCategoria.equals(categoriaActual)) {
                        categoriaActual = nombreCategoria;
                        Paragraph categoriaTitulo = new Paragraph(categoriaActual, categoriaFont);
                        categoriaTitulo.setSpacingBefore(14f);
                        categoriaTitulo.setSpacingAfter(6f);
                        document.add(categoriaTitulo);
                    }
                    agregarProducto(document, producto, nombreFont, descripcionFont, precioFont);
                }
            }

            document.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el catálogo en PDF", ex);
        }

        return baos.toByteArray();
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

                insertarImagen(workbook, drawing, producto.getFotoUrl(), numeroFila);
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

    // Fila de 3 columnas, siempre arrancando pegada al margen izquierdo real
    // de la página (nunca hay una columna vacía "fantasma" corriendo el
    // texto): [texto: nombre + subcategoría + descripción] [foto, si tiene]
    // [precio, alineado a la derecha del todo].
    private void agregarProducto(Document document, Producto producto, Font nombreFont,
            Font descripcionFont, Font precioFont) throws DocumentException {
        Image imagen = cargarImagen(producto.getFotoUrl());

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4f, 1.3f, 1.3f});
        table.setSpacingAfter(10f);

        PdfPCell textoCell = new PdfPCell();
        textoCell.setBorder(Rectangle.NO_BORDER);
        textoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        textoCell.addElement(new Paragraph(producto.getNombre(), nombreFont));
        if (producto.getDescripcion() != null && !producto.getDescripcion().isBlank()) {
            textoCell.addElement(new Paragraph(producto.getDescripcion(), descripcionFont));
        }
        table.addCell(textoCell);

        PdfPCell celdaImagen = imagen != null ? new PdfPCell(imagen) : new PdfPCell();
        celdaImagen.setBorder(Rectangle.NO_BORDER);
        celdaImagen.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celdaImagen.setHorizontalAlignment(Element.ALIGN_CENTER);
        if (imagen != null) {
            celdaImagen.setPadding(4f);
        }
        table.addCell(celdaImagen);

        PdfPCell celdaPrecio = new PdfPCell(new Paragraph(formatPrecio(producto.getPrecioVenta()), precioFont));
        celdaPrecio.setBorder(Rectangle.NO_BORDER);
        celdaPrecio.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celdaPrecio.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(celdaPrecio);

        document.add(table);
    }

    // La imagen vive en disco local, fuera de la transacción de BD. Si el
    // archivo no existe o falla al cargarse (corrupto, permisos, etc.) no
    // debe romper la generación del PDF entero: se omite solo esa miniatura.
    private Image cargarImagen(String fotoUrl) {
        if (fotoUrl == null || fotoUrl.isBlank()) {
            return null;
        }
        try {
            String nombreArchivo = fotoUrl.substring(fotoUrl.lastIndexOf('/') + 1);
            Path rutaImagen = UPLOAD_DIR.resolve(nombreArchivo);
            if (Files.exists(rutaImagen)) {
                Image imagen = Image.getInstance(rutaImagen.toString());
                imagen.scaleToFit(THUMBNAIL_SIZE, THUMBNAIL_SIZE);
                return imagen;
            }
        } catch (Exception ex) {
            // Imagen no cargable: seguimos sin ella, no interrumpimos el PDF.
        }
        return null;
    }

    private List<Producto> productosOrdenados(Long empresaId) {
        return productoRepository.findByEmpresaIdAndActivoTrue(empresaId).stream()
                .sorted(Comparator.<Producto, String>comparing(p -> p.getCategoria().getNombre(), String.CASE_INSENSITIVE_ORDER)
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
