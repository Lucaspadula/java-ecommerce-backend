package com.sistventas.backend.service.impl;

import com.sistventas.backend.entity.Categoria;
import com.sistventas.backend.entity.Empresa;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.exception.EmpresaNoEncontradaException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.EmpresaRepository;
import com.sistventas.backend.repository.ProductoRepository;
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

    // Mismo Path que CatalogoServiceImpl.UPLOAD_DIR: para cubrir las ramas de
    // insertarImagen/cargarImagen que SÍ tocan el filesystem (imagen
    // encontrada y reconocida) hace falta un archivo real en ese directorio,
    // relativo al cwd del test (la raíz del módulo backend). Se limpia en
    // @AfterEach para no ensuciar uploads/productos entre corridas.
    private static final Path UPLOAD_DIR = Paths.get("uploads", "productos");

    private final List<Path> archivosDePrueba = new ArrayList<>();

    private CatalogoServiceImpl service;

    private final UserPrincipal principal = new UserPrincipal(1L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    private void setUp() {
        service = new CatalogoServiceImpl(productoRepository, empresaRepository);
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
    void generarPdfConFotoRealEnDiscoIncluyeLaImagenEnLaTabla() throws IOException {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        Producto producto = producto("Mate", "Con foto", "Accesorios", "80.00");
        producto.setFotoUrl(crearImagenDePruebaEnDisco("pdf-con-foto.png"));

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
        producto.setFotoUrl("/uploads/productos/no-existe-en-disco.jpg");

        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));

        byte[] resultado = service.generarPdf(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarPdfConDescripcionEnBlancoNoLaAgregaComoParrafo() {
        setUp();
        Empresa empresa = empresaConNombre("Mi Tienda");
        Producto producto = producto("Mate", "   ", "Accesorios", "80.00");

        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));

        byte[] resultado = service.generarPdf(principal);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    void generarExcelConFotoRealEnDiscoInsertaLaImagen() throws IOException {
        setUp();
        Producto producto = producto("Mate", "Con foto", "Accesorios", "80.00");
        producto.setFotoUrl(crearImagenDePruebaEnDisco("excel-con-foto.png"));

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
        producto.setFotoUrl("/uploads/productos/no-existe.jpg");

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
        producto.setFotoUrl("/uploads/productos/foto.webp");

        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(producto));

        byte[] resultado = service.generarExcel(principal);

        assertThat(resultado).isNotEmpty();
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
