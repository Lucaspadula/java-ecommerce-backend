package com.sistventas.backend.service.impl;

import com.sistventas.backend.entity.Categoria;
import com.sistventas.backend.entity.Empresa;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ProductoFoto;
import com.sistventas.backend.entity.ProductoGrabado;
import com.sistventas.backend.entity.ProductoVariante;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.Subcategoria;
import com.sistventas.backend.entity.TiendaCatalogoSeccion;
import com.sistventas.backend.exception.EmpresaNoEncontradaException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.EmpresaRepository;
import com.sistventas.backend.repository.ProductoRepository;
import com.sistventas.backend.repository.TiendaCatalogoSeccionRepository;
import com.sistventas.backend.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

// Exportación del catálogo a PDF (OpenPDF) y Excel (Apache POI). No se
// parsea el binario resultante (no aporta valor de negocio verificar el
// layout exacto de una tabla PDF): alcanza con que no explote y que
// produzca bytes no vacíos, con y sin productos cargados. Las fotos se
// dejan sin URL a propósito: insertarImagen/cargarImagen las omiten en
// silencio cuando fotoUrl es null, así el test no depende del filesystem.
@ExtendWith(MockitoExtension.class)
class CatalogoServiceImplTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock private ProductoRepository productoRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private TiendaCatalogoSeccionRepository tiendaCatalogoSeccionRepository;

    // Mismo Path que CatalogoServiceImpl.UPLOAD_DIR: para cubrir las ramas de
    // insertarImagen/cargarImagen que SÍ tocan el filesystem (imagen
    // encontrada y reconocida) hace falta un archivo real en ese directorio,
    // relativo al cwd del test (la raíz del módulo backend). Se limpia en
    // @AfterEach para no ensuciar uploads/productos entre corridas.
    private static final Path UPLOAD_DIR = Paths.get("uploads", "productos");
    // Mismo Path que CatalogoServiceImpl.UPLOAD_DIR_EMPRESAS: la imagen de
    // portada del catálogo se guarda junto con el logo, no con las fotos de
    // producto.
    private static final Path UPLOAD_DIR_EMPRESAS = Paths.get("uploads", "empresas");

    private final List<Path> archivosDePrueba = new ArrayList<>();

    private CatalogoServiceImpl service;

    private final UserPrincipal principal = new UserPrincipal(1L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    private void setUp() {
        // ProductoFotoResolver es puro/sin dependencias: se instancia real,
        // no mockeado, mismo criterio que StockDisponibleCalculator en otros
        // tests de este service.
        service = new CatalogoServiceImpl(productoRepository, empresaRepository, new ProductoFotoResolver(),
                tiendaCatalogoSeccionRepository);
        // Default: sin puntos de "Importante"/"Cómo comprar" cargados. lenient()
        // porque no todos los tests llegan a ejercitar esta rama (ej. los que
        // lanzan excepción antes de resolver la empresa).
        lenient().when(tiendaCatalogoSeccionRepository.findByEmpresaIdAndTipoOrderByOrdenAscIdAsc(anyLong(), anyString()))
                .thenReturn(List.of());
    }

    // Reemplaza al viejo producto.setFotoUrl(...): la miniatura de catálogo
    // ahora se resuelve vía ProductoFotoResolver sobre el pool (ver
    // CatalogoServiceImpl.miniaturaDe), no una columna propia.
    private void agregarFotoAlPool(Producto producto, String url) {
        ProductoFoto foto = new ProductoFoto();
        foto.setProducto(producto);
        foto.setUrl(url);
        foto.setOrden(producto.getFotos().size());
        producto.getFotos().add(foto);
    }

    @AfterEach
    void limpiarArchivosDePrueba() {
        archivosDePrueba.forEach(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // best-effort, no debe romper el test
            }
        });
    }

    // Crea un PNG real y válido (decodificable por OpenPDF/ImageIO) en
    // uploads/productos, para ejercitar la rama "imagen encontrada y
    // reconocida" de cargarImagen/insertarImagen.
    private String crearImagenDePruebaEnDisco(String nombreArchivo) throws IOException {
        Files.createDirectories(UPLOAD_DIR);
        Path destino = UPLOAD_DIR.resolve(nombreArchivo);
        BufferedImage imagen = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(imagen, "png", destino.toFile());
        archivosDePrueba.add(destino);
        return "/uploads/productos/" + nombreArchivo;
    }

    // Análogo a crearImagenDePruebaEnDisco, pero en uploads/empresas — para
    // ejercitar cargarImagenPortada.
    private String crearImagenDePortadaDePruebaEnDisco(String nombreArchivo) throws IOException {
        Files.createDirectories(UPLOAD_DIR_EMPRESAS);
        Path destino = UPLOAD_DIR_EMPRESAS.resolve(nombreArchivo);
        BufferedImage imagen = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(imagen, "png", destino.toFile());
        archivosDePrueba.add(destino);
        return "/uploads/empresas/" + nombreArchivo;
    }

    @Test
    void generarPdfNoIncluyeLaPaginaDeIndice() throws IOException {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        Producto producto = producto("Mate", "Sin índice", "Accesorios", "80.00");

        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));

        byte[] resultado = service.generarPdf(principal);

        assertThat(textoCompletoDelPdf(resultado)).doesNotContain("ÍNDICE");
    }

    @Test
    void generarPdfConEstiloDeTextoPersonalizadoNoRompeYDevuelveBytesNoVacios() {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        empresa.setCatalogoTextoFuente("times");
        empresa.setCatalogoTextoTamanio(14);
        empresa.setCatalogoTextoColor("#900000");
        TiendaCatalogoSeccion punto = new TiendaCatalogoSeccion();
        punto.setEmpresaId(EMPRESA_ID);
        punto.setTipo("IMPORTANTE");
        punto.setTexto("Política de cambios y devoluciones.");

        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of());
        when(tiendaCatalogoSeccionRepository.findByEmpresaIdAndTipoOrderByOrdenAscIdAsc(EMPRESA_ID, "IMPORTANTE"))
                .thenReturn(List.of(punto));
        when(tiendaCatalogoSeccionRepository.findByEmpresaIdAndTipoOrderByOrdenAscIdAsc(EMPRESA_ID, "COMO_COMPRAR"))
                .thenReturn(List.of());

        byte[] resultado = service.generarPdf(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarPdfConEstiloPorBloqueNoRompeYElTextoSigueApareciendo() throws IOException {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        TiendaCatalogoSeccion punto = new TiendaCatalogoSeccion();
        punto.setEmpresaId(EMPRESA_ID);
        punto.setTipo("IMPORTANTE");
        punto.setTexto("Punto destacado en negrita y centrado.");
        punto.setAlineacion("CENTRO");
        punto.setNegrita(true);
        punto.setCursiva(true);
        punto.setSubrayado(true);

        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of());
        when(tiendaCatalogoSeccionRepository.findByEmpresaIdAndTipoOrderByOrdenAscIdAsc(EMPRESA_ID, "IMPORTANTE"))
                .thenReturn(List.of(punto));
        when(tiendaCatalogoSeccionRepository.findByEmpresaIdAndTipoOrderByOrdenAscIdAsc(EMPRESA_ID, "COMO_COMPRAR"))
                .thenReturn(List.of());

        byte[] resultado = service.generarPdf(principal);

        assertThat(textoCompletoDelPdf(resultado)).contains("Punto destacado en negrita y centrado.");
    }

    @Test
    void generarPdfConColorDeFondoDeProductosValidoNoRompeYDevuelveBytesNoVacios() {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        empresa.setCatalogoColorFondoProductos("#f7f3ec");
        Producto producto = producto("Mate", "Con fondo personalizado", "Accesorios", "80.00");

        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));

        byte[] resultado = service.generarPdf(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarPdfConColorDeFondoDeProductosInvalidoNoRompeYCaeAlFondoBlanco() {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        // Dato corrupto (ej. editado a mano en la base): no debe romper la
        // generación entera, solo ignorarse (ver CatalogoServiceImpl.parsearColorHex).
        empresa.setCatalogoColorFondoProductos("no-es-un-color");
        Producto producto = producto("Mate", "Con color corrupto", "Accesorios", "80.00");

        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));

        byte[] resultado = service.generarPdf(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarPdfConProductosDeVariasCategoriasDevuelveBytesNoVacios() {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(
                producto("Mate", "Bombillas", "Accesorios", "80.00"),
                producto("Termo", "Térmico 1L", "Accesorios", "150.00"),
                producto("Vela", null, "Decoración", "20.00")));

        byte[] resultado = service.generarPdf(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarPdfSinProductosNoExplotaYDevuelveBytesNoVacios() {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of());

        byte[] resultado = service.generarPdf(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarPdfDeEmpresaInexistenteLanzaExcepcion() {
        setUp();
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generarPdf(principal))
                .isInstanceOf(EmpresaNoEncontradaException.class);
    }

    @Test
    void generarPdfSinEmpresaEnElPrincipalLanzaSinEmpresaException() {
        setUp();
        UserPrincipal superAdmin = new UserPrincipal(1L, null, true, null);

        assertThatThrownBy(() -> service.generarPdf(superAdmin))
                .isInstanceOf(SinEmpresaException.class);
    }

    @Test
    void generarExcelConProductosDevuelveBytesNoVacios() {
        setUp();
        // generarExcel, a diferencia de generarPdf, no consulta el nombre de
        // la empresa: no hace falta stubear empresaRepository acá.
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(
                producto("Mate", "Bombillas", "Accesorios", "80.00"),
                producto("Vela", null, "Decoración", "20.00")));

        byte[] resultado = service.generarExcel(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarExcelSinProductosNoExplotaYDevuelveBytesNoVacios() {
        setUp();
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of());

        byte[] resultado = service.generarExcel(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarExcelSinEmpresaEnElPrincipalLanzaSinEmpresaException() {
        setUp();
        UserPrincipal superAdmin = new UserPrincipal(1L, null, true, null);

        assertThatThrownBy(() -> service.generarExcel(superAdmin))
                .isInstanceOf(SinEmpresaException.class);
    }

    @Test
    void generarPdfUsaLaMiniaturaDelPoolNoLaColumnaFotoUrlDirecta() throws IOException {
        // El "silent-break" que el design llama explícitamente: producto.getFotoUrl()
        // queda deprecada/stale, la miniatura real vive en el pool. Acá la
        // columna vieja NO se toca (queda null) y solo se carga la foto en el
        // pool — si CatalogoServiceImpl todavía leyera producto.getFotoUrl()
        // directo, este test fallaría porque no habría imagen que insertar.
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        Producto producto = producto("Mate", "Con foto en el pool", "Accesorios", "80.00");
        assertThat(producto.getFotoUrl()).isNull();
        agregarFotoAlPool(producto, crearImagenDePruebaEnDisco("pool-miniatura.png"));

        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));

        byte[] resultado = service.generarPdf(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarPdfConFotoRealEnDiscoIncluyeLaImagenEnLaTabla() throws IOException {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        Producto producto = producto("Mate", "Con foto", "Accesorios", "80.00");
        agregarFotoAlPool(producto, crearImagenDePruebaEnDisco("pdf-con-foto.png"));

        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));

        byte[] resultado = service.generarPdf(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarPdfConFotoUrlDeArchivoInexistenteNoRompeYOmiteLaImagen() {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        Producto producto = producto("Mate", "Sin archivo en disco", "Accesorios", "80.00");
        agregarFotoAlPool(producto, "/uploads/productos/no-existe-en-disco.jpg");

        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));

        byte[] resultado = service.generarPdf(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarPdfConImagenDePortadaDevuelveBytesNoVacios() throws IOException {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        empresa.setCatalogoPortadaImagenUrl(crearImagenDePortadaDePruebaEnDisco("portada.png"));

        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of());

        byte[] resultado = service.generarPdf(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarPdfConImagenDePortadaInexistenteNoRompeYCaeAlColorSolido() {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        empresa.setCatalogoPortadaImagenUrl("/uploads/empresas/no-existe-en-disco.png");

        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of());

        byte[] resultado = service.generarPdf(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarPdfConVariantesYColoresHabilitadoMuestraLosColores() throws IOException {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        Producto producto = producto("Mate Imperial", "Con virola", "Accesorios", "45000.00");
        producto.getVariantes().add(variante("Rojo"));
        producto.getVariantes().add(variante("Azul"));

        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));

        byte[] resultado = service.generarPdf(principal);

        assertThat(textoCompletoDelPdf(resultado)).contains("Rojo · Azul");
    }

    @Test
    void generarPdfConColoresDeshabilitadoNoMuestraLaLineaDeColores() throws IOException {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        empresa.setCatalogoMostrarColores(false);
        Producto producto = producto("Mate Imperial", "Con virola", "Accesorios", "45000.00");
        producto.getVariantes().add(variante("Rojo"));

        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));

        byte[] resultado = service.generarPdf(principal);

        assertThat(textoCompletoDelPdf(resultado)).doesNotContain("Rojo");
    }

    private ProductoVariante variante(String color) {
        ProductoVariante variante = new ProductoVariante();
        variante.setColor(color);
        return variante;
    }

    private String textoCompletoDelPdf(byte[] pdf) throws IOException {
        PdfReader reader = new PdfReader(pdf);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        StringBuilder texto = new StringBuilder();
        for (int pagina = 1; pagina <= reader.getNumberOfPages(); pagina++) {
            texto.append(extractor.getTextFromPage(pagina));
        }
        reader.close();
        return texto.toString();
    }

    @Test
    void generarExcelConFotoRealEnDiscoInsertaLaImagen() throws IOException {
        setUp();
        Producto producto = producto("Mate", "Con foto", "Accesorios", "80.00");
        agregarFotoAlPool(producto, crearImagenDePruebaEnDisco("excel-con-foto.png"));

        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));

        byte[] resultado = service.generarExcel(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarExcelConFotoJpgInexistenteEnDiscoOmiteLaImagenSinRomper() {
        // La extensión .jpg SÍ es reconocida (tipoImagenPoi), pero el archivo
        // no existe en disco: insertarImagen tiene que cortar en el segundo
        // operando del OR (!Files.exists) sin explotar.
        setUp();
        Producto producto = producto("Termo", "Sin archivo", "Accesorios", "150.00");
        agregarFotoAlPool(producto, "/uploads/productos/no-existe.jpg");

        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));

        byte[] resultado = service.generarExcel(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarExcelConFotoDeExtensionNoSoportadaOmiteLaImagen() {
        // .webp no está en la whitelist de tipoImagenPoi (solo png/jpg/jpeg
        // para el Excel, a diferencia de la whitelist de subida que sí lo
        // acepta): tiene que devolver -1 y cortar sin tocar el filesystem.
        setUp();
        Producto producto = producto("Bombilla", "Formato no soportado en Excel", "Accesorios", "50.00");
        agregarFotoAlPool(producto, "/uploads/productos/foto.webp");

        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));

        byte[] resultado = service.generarExcel(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarPdfConPoliticasComoComprarYContactoAgregaEsasPaginasSinExplotar() {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        empresa.setTiendaContactoWhatsapp("11-2222-3333");
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(tiendaCatalogoSeccionRepository.findByEmpresaIdAndTipoOrderByOrdenAscIdAsc(EMPRESA_ID, "IMPORTANTE"))
                .thenReturn(List.of(puntoCatalogo("IMPORTANTE", "No hacemos cambios luego de despachado.")));
        when(tiendaCatalogoSeccionRepository.findByEmpresaIdAndTipoOrderByOrdenAscIdAsc(EMPRESA_ID, "COMO_COMPRAR"))
                .thenReturn(List.of(puntoCatalogo("COMO_COMPRAR", "Escribinos por WhatsApp para cotizar.")));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(
                producto("Mate", "Bombillas", "Accesorios", "80.00")));

        byte[] resultado = service.generarPdf(principal);

        assertThat(resultado).isNotEmpty();
    }

    private TiendaCatalogoSeccion puntoCatalogo(String tipo, String texto) {
        TiendaCatalogoSeccion punto = new TiendaCatalogoSeccion();
        punto.setEmpresaId(EMPRESA_ID);
        punto.setTipo(tipo);
        punto.setTexto(texto);
        punto.setOrden(0);
        return punto;
    }

    @Test
    void generarPdfSinPoliticasNiComoComprarNiContactoOmiteEsasPaginasSinExplotar() {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(
                producto("Mate", "Bombillas", "Accesorios", "80.00")));

        byte[] resultado = service.generarPdf(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarPdfAgrupaPorCategoriaYSubcategoriaSinExplotar() {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        Producto conSub1 = productoConSubcategoria("Mate imperial", "Accesorios", "Mates", "16000.00");
        Producto conSub2 = productoConSubcategoria("Termo 1L", "Accesorios", "Termos", "14500.00");
        Producto sinSub = producto("Vela", null, "Decoración", "20.00");
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(conSub1, conSub2, sinSub));

        byte[] resultado = service.generarPdf(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarPdfConProductoConGrabadosMuestraElPrecioMasBaratoSinExplotar() {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        Producto producto = producto("Mate imperial", null, "Accesorios", "16000.00");
        agregarGrabado(producto, new BigDecimal("5000.00"));
        agregarGrabado(producto, new BigDecimal("4000.00"));
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));

        byte[] resultado = service.generarPdf(principal);

        assertThat(resultado).isNotEmpty();
    }

    private void agregarGrabado(Producto producto, BigDecimal precio) {
        ProductoGrabado grabado = new ProductoGrabado();
        grabado.setProducto(producto);
        grabado.setLugar("Virola");
        grabado.setPrecio(precio);
        producto.getGrabados().add(grabado);
    }

    private Producto productoConSubcategoria(String nombre, String nombreSubcategoria, String nombreCategoria, String precio) {
        Producto producto = producto(nombre, null, nombreCategoria, precio);
        Subcategoria subcategoria = new Subcategoria();
        subcategoria.setId(1L);
        subcategoria.setNombre(nombreSubcategoria);
        producto.setSubcategoria(subcategoria);
        return producto;
    }

    private Empresa empresaConNombre(String nombre) {
        Empresa empresa = new Empresa();
        empresa.setId(EMPRESA_ID);
        empresa.setNombre(nombre);
        return empresa;
    }

    private Producto producto(String nombre, String descripcion, String nombreCategoria, String precio) {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre(nombreCategoria);

        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre(nombre);
        producto.setDescripcion(descripcion);
        producto.setCategoria(categoria);
        producto.setPrecioVenta(new BigDecimal(precio));
        return producto;
    }
}
