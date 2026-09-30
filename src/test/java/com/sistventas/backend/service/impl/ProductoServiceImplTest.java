package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarProductoRequest;
import com.sistventas.backend.dto.AjustePrecioCategoriaRequest;
import com.sistventas.backend.dto.AjustePrecioCategoriaResultadoDto;
import com.sistventas.backend.dto.CrearResenaRequest;
import com.sistventas.backend.dto.FotoUploadDto;
import com.sistventas.backend.dto.ProductoComponenteRequest;
import com.sistventas.backend.dto.ProductoDto;
import com.sistventas.backend.dto.ProductoGrabadoDto;
import com.sistventas.backend.dto.ProductoGrabadoRequest;
import com.sistventas.backend.dto.ProductoInsumoRequest;
import com.sistventas.backend.dto.ProductoRequest;
import com.sistventas.backend.dto.ProductoVarianteRequest;
import com.sistventas.backend.dto.ResenaDto;
import com.sistventas.backend.dto.TipoAjustePrecio;
import com.sistventas.backend.entity.Categoria;
import com.sistventas.backend.entity.Insumo;
import com.sistventas.backend.entity.Producto;
import com.sistventas.backend.entity.ProductoComponente;
import com.sistventas.backend.entity.ProductoFoto;
import com.sistventas.backend.entity.ProductoGrabado;
import com.sistventas.backend.entity.ProductoVariante;
import com.sistventas.backend.entity.Resena;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.Subcategoria;
import com.sistventas.backend.exception.ArchivoInvalidoException;
import com.sistventas.backend.exception.ProductoComponenteInvalidoException;
import com.sistventas.backend.exception.ProductoFotoNoEncontradaException;
import com.sistventas.backend.exception.ProductoNoEncontradoException;
import com.sistventas.backend.exception.ResenaNoEncontradaException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.exception.SubcategoriaNoEncontradaException;
import com.sistventas.backend.repository.AtributoFiltroRepository;
import com.sistventas.backend.repository.AtributoFiltroValorRepository;
import com.sistventas.backend.repository.CategoriaRepository;
import com.sistventas.backend.repository.InsumoRepository;
import com.sistventas.backend.repository.ProductoRepository;
import com.sistventas.backend.repository.ResenaRepository;
import com.sistventas.backend.repository.SubcategoriaRepository;
import com.sistventas.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests de la lógica de negocio de ProductoServiceImpl: kits (Composite),
// costo efectivo y persistencia de stock/costoUnitario propios, y reemplazo
// de variantes. No es @SpringBootTest: todos los repositorios se mockean,
// el service se ejercita directo vía crear()/actualizar() porque los métodos
// bajo prueba (aplicarComponentes, costoEfectivo, aplicarVariantes) son
// privados.
@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    private static final Long EMPRESA_ID = 1L;
    private static final Long CATEGORIA_ID = 10L;

    @Mock private ProductoRepository productoRepository;
    @Mock private InsumoRepository insumoRepository;
    @Mock private ResenaRepository resenaRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private SubcategoriaRepository subcategoriaRepository;
    @Mock private AtributoFiltroRepository atributoFiltroRepository;
    @Mock private AtributoFiltroValorRepository atributoFiltroValorRepository;
    @Mock private StockDisponibleCalculator stockDisponibleCalculator;
    @Mock private ImagenUploadValidator imagenUploadValidator;

    // Puro/sin dependencias: se usa la implementación REAL, mismo criterio
    // que StockDisponibleCalculator en PublicTiendaServiceImplTest — acá SÍ
    // hace falta la regla de negocio de verdad (varios tests nuevos de fotos
    // dependen de resolverMiniatura/fotosDeVariante reales).
    private final ProductoFotoResolver productoFotoResolver = new ProductoFotoResolver();

    private ProductoServiceImpl service;

    private final UserPrincipal principal = new UserPrincipal(1L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @BeforeEach
    void setUp() {
        service = new ProductoServiceImpl(productoRepository, insumoRepository, resenaRepository,
                categoriaRepository, subcategoriaRepository, atributoFiltroRepository, atributoFiltroValorRepository,
                stockDisponibleCalculator, imagenUploadValidator, productoFotoResolver);

        Categoria categoria = new Categoria();
        categoria.setId(CATEGORIA_ID);
        categoria.setNombre("General");
        // lenient: no todos los tests necesariamente disparan el camino que
        // lee la categoría (ej. algunos cortan antes por una excepción), pero
        // cuando la disparan siempre es la misma categoría válida.
        lenient().when(categoriaRepository.findByIdAndEmpresaId(anyLong(), org.mockito.ArgumentMatchers.eq(EMPRESA_ID)))
                .thenReturn(Optional.of(categoria));
        // save() devuelve el mismo objeto que recibe: alcanza para poder
        // inspeccionar el Producto armado por el service sin necesitar un id
        // real generado por la base.
        lenient().when(productoRepository.save(org.mockito.ArgumentMatchers.any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- aplicarComponentes / kits (Composite) ---

    @Test
    void actualizarConComponenteQueEsElMismoProductoLanzaExcepcion() {
        Producto objetivo = productoConId(5L);
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(objetivo));

        ActualizarProductoRequest request = requestActualizarConComponentes(
                List.of(new ProductoComponenteRequest(5L, 1)));

        assertThatThrownBy(() -> service.actualizar(5L, request, principal))
                .isInstanceOf(ProductoComponenteInvalidoException.class)
                .hasMessageContaining("no puede tenerse a sí mismo");
    }

    @Test
    void actualizarConComponenteQueYaEsUnKitLanzaExcepcion() {
        // El kit anidado no es solo una validación arbitraria: StockDisponibleCalculator.calcularKit
        // no es recursivo, así que permitirlo dejaría el cálculo de disponible roto para ese caso.
        Producto objetivo = productoConId(5L);
        Producto comboExistente = productoConId(7L);
        comboExistente.setNombre("Combo Mate + Termo");
        ProductoComponente componenteInterno = new ProductoComponente();
        componenteInterno.setProducto(comboExistente);
        componenteInterno.setComponenteProducto(productoConId(1L));
        comboExistente.getComponentes().add(componenteInterno);

        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(objetivo));
        when(productoRepository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.of(comboExistente));

        ActualizarProductoRequest request = requestActualizarConComponentes(
                List.of(new ProductoComponenteRequest(7L, 1)));

        assertThatThrownBy(() -> service.actualizar(5L, request, principal))
                .isInstanceOf(ProductoComponenteInvalidoException.class)
                .hasMessageContaining("Combo Mate + Termo")
                .hasMessageContaining("ya es un kit");
    }

    @Test
    void actualizarConComponenteInexistenteLanzaProductoNoEncontrado() {
        Producto objetivo = productoConId(5L);
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(objetivo));
        when(productoRepository.findByIdAndEmpresaId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

        ActualizarProductoRequest request = requestActualizarConComponentes(
                List.of(new ProductoComponenteRequest(99L, 1)));

        assertThatThrownBy(() -> service.actualizar(5L, request, principal))
                .isInstanceOf(ProductoNoEncontradoException.class);
    }

    @Test
    void crearConComponentesNullDejaProductoSinComponentes() {
        ProductoRequest request = requestConComponentes(null);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        service.crear(request, principal);
        verify(productoRepository).save(captor.capture());

        assertThat(captor.getValue().getComponentes()).isEmpty();
    }

    @Test
    void crearConComponentesVaciosDejaProductoSinComponentes() {
        ProductoRequest request = requestConComponentes(List.of());

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        service.crear(request, principal);
        verify(productoRepository).save(captor.capture());

        assertThat(captor.getValue().getComponentes()).isEmpty();
    }

    @Test
    void crearConDosComponentesValidosLosPersisteConSuCantidad() {
        Producto mate = productoConId(20L);
        Producto termo = productoConId(21L);
        when(productoRepository.findByIdAndEmpresaId(20L, EMPRESA_ID)).thenReturn(Optional.of(mate));
        when(productoRepository.findByIdAndEmpresaId(21L, EMPRESA_ID)).thenReturn(Optional.of(termo));

        ProductoRequest request = requestConComponentes(List.of(
                new ProductoComponenteRequest(20L, 2),
                new ProductoComponenteRequest(21L, 3)));

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        service.crear(request, principal);
        verify(productoRepository).save(captor.capture());

        assertThat(captor.getValue().getComponentes())
                .extracting(ProductoComponente::getComponenteProducto, ProductoComponente::getCantidad)
                .containsExactlyInAnyOrder(
                        tuple(mate, 2),
                        tuple(termo, 3));
    }

    // --- costoEfectivo ---

    @Test
    void productoConRecetaSumaCostoUnitarioPropioMasElCostoDeLosInsumos() {
        Insumo insumo = new Insumo();
        insumo.setId(1L);
        insumo.setNombre("Madera");
        insumo.setCostoUnitario(new BigDecimal("5.00"));
        insumo.setStock(new BigDecimal("100"));
        when(insumoRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(insumo));

        ProductoRequest request = new ProductoRequest("Mate", CATEGORIA_ID, null, null,
                new BigDecimal("100.00"), null, null,
                List.of(new ProductoInsumoRequest(1L, new BigDecimal("2"))),
                null, null, null, null, new BigDecimal("999.00"), null);

        ProductoDto dto = service.crear(request, principal);

        assertThat(dto.costoUnitario()).isEqualByComparingTo("1009.00");
        // costoPropio es el valor crudo persistido (sin sumarle los
        // insumos) — lo usa el frontend para repoblar "Costo propio" al
        // editar. Si acá devolviera el mismo total que costoUnitario, el
        // form al editar duplicaría el costo de los insumos.
        assertThat(dto.costoPropio()).isEqualByComparingTo("999.00");
    }

    @Test
    void productoSinRecetaUsaElCostoUnitarioCargadoAMano() {
        ProductoRequest request = new ProductoRequest("Vela", CATEGORIA_ID, null, null,
                new BigDecimal("50.00"), null, null, null, null, null, null, null, new BigDecimal("15.50"), null);

        ProductoDto dto = service.crear(request, principal);

        assertThat(dto.costoUnitario()).isEqualByComparingTo("15.50");
    }

    @Test
    void productoSinRecetaYSinCostoUnitarioDaCero() {
        ProductoRequest request = new ProductoRequest("Vela", CATEGORIA_ID, null, null,
                new BigDecimal("50.00"), null, null, null, null, null, null, null, null, null);

        ProductoDto dto = service.crear(request, principal);

        assertThat(dto.costoUnitario()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --- stock/costoUnitario propio ---

    @Test
    void alCrearSinInsumosNiComponentesPersisteStockYCostoDelRequest() {
        ProductoRequest request = new ProductoRequest("Vela", CATEGORIA_ID, null, null,
                new BigDecimal("50.00"), null, null, null, null, null, null, 25, new BigDecimal("12.34"), null);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        service.crear(request, principal);
        verify(productoRepository).save(captor.capture());

        assertThat(captor.getValue().getStock()).isEqualTo(25);
        assertThat(captor.getValue().getCostoUnitario()).isEqualByComparingTo("12.34");
    }

    @Test
    void alCrearSinStockUsaCeroPorDefecto() {
        ProductoRequest request = new ProductoRequest("Vela", CATEGORIA_ID, null, null,
                new BigDecimal("50.00"), null, null, null, null, null, null, null, null, null);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        service.crear(request, principal);
        verify(productoRepository).save(captor.capture());

        assertThat(captor.getValue().getStock()).isEqualTo(0);
    }

    // --- aplicarVariantes ---

    @Test
    void actualizarVariantesReemplazaListaCompleta() {
        // Merge por id (no clear + recrear todo, a diferencia de insumos):
        // la variante con id 100 se actualiza in-place, la que no trae id es
        // nueva. Acá solo se verifica el resultado final de la lista.
        Producto existente = productoConId(5L);
        ProductoVariante rojo = new ProductoVariante();
        rojo.setId(100L);
        rojo.setProducto(existente);
        rojo.setColor("Rojo");
        rojo.setStock(3);
        existente.getVariantes().add(rojo);

        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));

        ActualizarProductoRequest request = new ActualizarProductoRequest("Mate", CATEGORIA_ID, null, null,
                new BigDecimal("100.00"), null, null, null,
                List.of(new ProductoVarianteRequest(100L, "Azul", 9, null),
                        new ProductoVarianteRequest(null, "Verde", 4, null)),
                null, null, null, null, null);

        service.actualizar(5L, request, principal);

        assertThat(existente.getVariantes())
                .extracting(ProductoVariante::getColor, ProductoVariante::getStock)
                .containsExactlyInAnyOrder(
                        tuple("Azul", 9),
                        tuple("Verde", 4));
    }

    @Test
    void actualizarVarianteExistenteNoBorraSuFotoUrlLegacy() {
        // Regresión: ProductoVarianteRequest ya no trae fotoUrl (la foto vive
        // en producto_foto, ver ProductoFotoResolver) — aplicarVariantes NO
        // debe tocar producto_variante.foto_url en absoluto. Esa columna
        // queda deprecated como red de seguridad de rollback (ver V43); antes
        // de este fix, cada guardado la pisaba con null porque el request ya
        // no mandaba el campo.
        Producto existente = productoConId(5L);
        ProductoVariante rojo = new ProductoVariante();
        rojo.setId(100L);
        rojo.setProducto(existente);
        rojo.setColor("Rojo");
        rojo.setStock(3);
        rojo.setFotoUrl("/uploads/productos/variante-rojo.jpg");
        existente.getVariantes().add(rojo);

        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));

        ActualizarProductoRequest request = new ActualizarProductoRequest("Mate", CATEGORIA_ID, null, null,
                new BigDecimal("100.00"), null, null, null,
                List.of(new ProductoVarianteRequest(100L, "Rojo", 5, null)),
                null, null, null, null, null);

        service.actualizar(5L, request, principal);

        assertThat(existente.getVariantes())
                .extracting(ProductoVariante::getFotoUrl)
                .containsExactly("/uploads/productos/variante-rojo.jpg");
    }

    @Test
    void actualizarConVariantesNullDejaProductoSinVariantes() {
        Producto existente = productoConId(5L);
        ProductoVariante variante = new ProductoVariante();
        variante.setId(100L);
        variante.setProducto(existente);
        variante.setColor("Rojo");
        variante.setStock(3);
        existente.getVariantes().add(variante);

        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));

        ActualizarProductoRequest request = new ActualizarProductoRequest("Mate", CATEGORIA_ID, null, null,
                new BigDecimal("100.00"), null, null, null, null, null, null, null, null, null);

        service.actualizar(5L, request, principal);

        assertThat(existente.getVariantes()).isEmpty();
    }

    // --- ajustarPrecioPorCategoria ---

    @Test
    void ajustarPrecioPorCategoriaConPorcentajeAplicaElAumentoATodosLosProductosDeLaCategoria() {
        Producto p1 = productoConId(1L);
        p1.setPrecioVenta(new BigDecimal("100.00"));
        Producto p2 = productoConId(2L);
        p2.setPrecioVenta(new BigDecimal("200.00"));
        when(productoRepository.findByEmpresaIdAndCategoriaIdAndActivoTrue(EMPRESA_ID, CATEGORIA_ID))
                .thenReturn(List.of(p1, p2));

        AjustePrecioCategoriaRequest request =
                new AjustePrecioCategoriaRequest(CATEGORIA_ID, TipoAjustePrecio.PORCENTAJE, new BigDecimal("10"));

        AjustePrecioCategoriaResultadoDto resultado = service.ajustarPrecioPorCategoria(request, principal);

        assertThat(resultado.productosActualizados()).isEqualTo(2);
        assertThat(p1.getPrecioVenta()).isEqualByComparingTo("110.00");
        assertThat(p2.getPrecioVenta()).isEqualByComparingTo("220.00");
    }

    @Test
    void ajustarPrecioPorCategoriaConMontoFijoNegativoNuncaDejaElPrecioPorDebajoDeCero() {
        Producto producto = productoConId(1L);
        producto.setPrecioVenta(new BigDecimal("30.00"));
        when(productoRepository.findByEmpresaIdAndCategoriaIdAndActivoTrue(EMPRESA_ID, CATEGORIA_ID))
                .thenReturn(List.of(producto));

        AjustePrecioCategoriaRequest request =
                new AjustePrecioCategoriaRequest(CATEGORIA_ID, TipoAjustePrecio.MONTO_FIJO, new BigDecimal("-50"));

        service.ajustarPrecioPorCategoria(request, principal);

        assertThat(producto.getPrecioVenta()).isEqualByComparingTo("0.00");
    }

    @Test
    void ajustarPrecioPorCategoriaSinProductosDaCeroActualizadosSinError() {
        when(productoRepository.findByEmpresaIdAndCategoriaIdAndActivoTrue(EMPRESA_ID, CATEGORIA_ID))
                .thenReturn(List.of());

        AjustePrecioCategoriaRequest request =
                new AjustePrecioCategoriaRequest(CATEGORIA_ID, TipoAjustePrecio.PORCENTAJE, new BigDecimal("10"));

        AjustePrecioCategoriaResultadoDto resultado = service.ajustarPrecioPorCategoria(request, principal);

        assertThat(resultado.productosActualizados()).isZero();
    }

    // --- reseñas ---

    @Test
    void crearResenaLaPersisteAsociadaAlProductoYALaEmpresa() {
        Producto producto = productoConId(5L);
        producto.setEmpresaId(EMPRESA_ID);
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(resenaRepository.save(any(Resena.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CrearResenaRequest request = new CrearResenaRequest("Juana", "Excelente producto");

        ResenaDto resultado = service.crearResena(5L, request, principal);

        assertThat(resultado.clienteNombre()).isEqualTo("Juana");
        assertThat(resultado.comentario()).isEqualTo("Excelente producto");

        ArgumentCaptor<Resena> captor = ArgumentCaptor.forClass(Resena.class);
        verify(resenaRepository).save(captor.capture());
        assertThat(captor.getValue().getProductoId()).isEqualTo(5L);
        assertThat(captor.getValue().getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    void listarResenasDevuelveSoloLasDelProductoPedido() {
        Producto producto = productoConId(5L);
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        Resena resena = new Resena();
        resena.setId(1L);
        resena.setClienteNombre("Pedro");
        resena.setComentario("Muy bueno");
        resena.setFecha(LocalDateTime.now());
        when(resenaRepository.findByProductoIdOrderByFechaDesc(5L)).thenReturn(List.of(resena));

        List<ResenaDto> resultado = service.listarResenas(5L, principal);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).clienteNombre()).isEqualTo("Pedro");
    }

    @Test
    void listarResenasDeProductoDeOtraEmpresaLanzaProductoNoEncontrado() {
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarResenas(5L, principal))
                .isInstanceOf(ProductoNoEncontradoException.class);

        verify(resenaRepository, never()).findByProductoIdOrderByFechaDesc(anyLong());
    }

    // --- fotos del producto: pool unificado (agregarFoto/eliminarFoto/reordenarFotos/asignarColorFoto) ---

    @Test
    void agregarFotoConMenosDe8LaPersisteConElSiguienteOrdenYSinColor() {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(imagenUploadValidator.validarYObtenerExtension(any())).thenReturn(".jpg");

        ProductoDto resultado = service.agregarFoto(5L, mock(MultipartFile.class), null, principal);

        assertThat(producto.getFotos()).hasSize(1);
        assertThat(producto.getFotos().get(0).getUrl()).startsWith("/uploads/productos/");
        assertThat(producto.getFotos().get(0).getOrden()).isZero();
        assertThat(producto.getFotos().get(0).getVarianteId()).isNull();
        assertThat(resultado.fotos()).hasSize(1);
    }

    @Test
    void agregarFotoConVarianteIdValidaLaAsociaAEseColor() {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        ProductoVariante rojo = new ProductoVariante();
        rojo.setId(20L);
        rojo.setProducto(producto);
        rojo.setColor("Rojo");
        producto.getVariantes().add(rojo);
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(imagenUploadValidator.validarYObtenerExtension(any())).thenReturn(".jpg");

        service.agregarFoto(5L, mock(MultipartFile.class), 20L, principal);

        assertThat(producto.getFotos().get(0).getVarianteId()).isEqualTo(20L);
    }

    @Test
    void agregarFotoConVarianteIdQueNoPerteneceAlProductoLanzaExcepcionYNoEscribeNadaEnElPool() {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.agregarFoto(5L, mock(MultipartFile.class), 999L, principal))
                .isInstanceOf(ProductoNoEncontradoException.class);

        assertThat(producto.getFotos()).isEmpty();
        verify(imagenUploadValidator, never()).validarYObtenerExtension(any());
    }

    @Test
    void agregarFotoConElPoolYaEn8RechazaAntesDeEscribirNadaEnDisco() {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        for (int i = 0; i < 8; i++) {
            producto.getFotos().add(fotoDePool(producto, (long) i, null, "/f" + i + ".jpg", i));
        }
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.agregarFoto(5L, mock(MultipartFile.class), null, principal))
                .isInstanceOf(ArchivoInvalidoException.class)
                .hasMessageContaining("8");

        // El chequeo del tope corta ANTES de validar/escribir el archivo —
        // ver design Open Question (cap-then-write ordering).
        verify(imagenUploadValidator, never()).validarYObtenerExtension(any());
        assertThat(producto.getFotos()).hasSize(8);
    }

    @Test
    void eliminarFotoLaBorraYReindexaElOrdenDeLasRestantes() {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        ProductoFoto f0 = fotoDePool(producto, 1L, null, "/uploads/productos/f0.jpg", 0);
        ProductoFoto f1 = fotoDePool(producto, 2L, null, "https://cdn.externo.com/f1.jpg", 1);
        ProductoFoto f2 = fotoDePool(producto, 3L, null, "/uploads/productos/f2.jpg", 2);
        producto.getFotos().addAll(List.of(f0, f1, f2));
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        ProductoDto resultado = service.eliminarFoto(5L, 1L, principal);

        assertThat(producto.getFotos()).hasSize(2);
        assertThat(producto.getFotos()).extracting(ProductoFoto::getId, ProductoFoto::getOrden)
                .containsExactlyInAnyOrder(tuple(2L, 0), tuple(3L, 1));
        assertThat(resultado.fotos()).hasSize(2);
    }

    @Test
    void eliminarFotoQueNoPerteneceAlProductoLanza404() {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.eliminarFoto(5L, 999L, principal))
                .isInstanceOf(ProductoFotoNoEncontradaException.class);
    }

    @Test
    void reordenarFotosReasignaOrdenSegunLaPosicionDeLaListaRecibida() {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        ProductoFoto f0 = fotoDePool(producto, 1L, null, "/a.jpg", 0);
        ProductoFoto f1 = fotoDePool(producto, 2L, null, "/b.jpg", 1);
        ProductoFoto f2 = fotoDePool(producto, 3L, null, "/c.jpg", 2);
        producto.getFotos().addAll(List.of(f0, f1, f2));
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        service.reordenarFotos(5L, List.of(3L, 1L, 2L), principal);

        assertThat(f2.getOrden()).isZero();
        assertThat(f0.getOrden()).isEqualTo(1);
        assertThat(f1.getOrden()).isEqualTo(2);
    }

    @Test
    void reordenarFotosConIdDeOtroProductoRechazaTodaLaOperacion() {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        ProductoFoto f0 = fotoDePool(producto, 1L, null, "/a.jpg", 0);
        producto.getFotos().add(f0);
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.reordenarFotos(5L, List.of(1L, 999L), principal))
                .isInstanceOf(ProductoFotoNoEncontradaException.class);

        // La foto propia no debe quedar reordenada a mitad de camino.
        assertThat(f0.getOrden()).isZero();
    }

    @Test
    void asignarColorFotoReasignaElVarianteIdSinTocarLaUrl() {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        ProductoVariante azul = new ProductoVariante();
        azul.setId(21L);
        azul.setProducto(producto);
        azul.setColor("Azul");
        producto.getVariantes().add(azul);
        ProductoFoto foto = fotoDePool(producto, 1L, 20L, "/rojo-a-azul.jpg", 0);
        producto.getFotos().add(foto);
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        service.asignarColorFoto(5L, 1L, 21L, principal);

        assertThat(foto.getVarianteId()).isEqualTo(21L);
        assertThat(foto.getUrl()).isEqualTo("/rojo-a-azul.jpg");
    }

    @Test
    void asignarColorFotoConVarianteIdNullQuitaElColor() {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        ProductoFoto foto = fotoDePool(producto, 1L, 20L, "/con-color.jpg", 0);
        producto.getFotos().add(foto);
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        service.asignarColorFoto(5L, 1L, null, principal);

        assertThat(foto.getVarianteId()).isNull();
    }

    @Test
    void asignarColorFotoDeUnaFotoQueNoExisteLanza404() {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.asignarColorFoto(5L, 999L, null, principal))
                .isInstanceOf(ProductoFotoNoEncontradaException.class);
    }

    // --- ajustarFoto: toggle manual de contain/cover por foto ---

    @Test
    void ajustarFotoConAgrandadaTrueLoPersisteYLoExponeEnElDto() {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        ProductoFoto foto = fotoDePool(producto, 1L, null, "/a.jpg", 0);
        producto.getFotos().add(foto);
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        ProductoDto resultado = service.ajustarFoto(5L, 1L, true, principal);

        assertThat(foto.isAgrandada()).isTrue();
        assertThat(resultado.fotos().get(0).agrandada()).isTrue();
    }

    @Test
    void ajustarFotoConAgrandadaFalseLaVuelveAContain() {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        ProductoFoto foto = fotoDePool(producto, 1L, null, "/a.jpg", 0);
        foto.setAgrandada(true);
        producto.getFotos().add(foto);
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        service.ajustarFoto(5L, 1L, false, principal);

        assertThat(foto.isAgrandada()).isFalse();
    }

    @Test
    void ajustarFotoDeUnaFotoQueNoExisteLanza404() {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.ajustarFoto(5L, 999L, true, principal))
                .isInstanceOf(ProductoFotoNoEncontradaException.class);
    }

    // --- toDto: miniatura derivada del pool (ProductoFotoResolver) ---

    @Test
    void obtenerConFotosMixtasResuelveFotoUrlComoLaPrimeraSinColor() {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        producto.getFotos().add(fotoDePool(producto, 1L, 20L, "/con-color.jpg", 0));
        producto.getFotos().add(fotoDePool(producto, 2L, null, "/general.jpg", 1));
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        ProductoDto resultado = service.obtener(5L, principal);

        assertThat(resultado.fotoUrl()).isEqualTo("/general.jpg");
    }

    @Test
    void obtenerConVarianteExponeSuFotoPropiaDelPoolComoFotoUrlDeLaVariante() {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        ProductoVariante rojo = new ProductoVariante();
        rojo.setId(20L);
        rojo.setProducto(producto);
        rojo.setColor("Rojo");
        producto.getVariantes().add(rojo);
        producto.getFotos().add(fotoDePool(producto, 1L, 20L, "/rojo.jpg", 0));
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        ProductoDto resultado = service.obtener(5L, principal);

        assertThat(resultado.variantes().get(0).fotoUrl()).isEqualTo("/rojo.jpg");
    }

    private ProductoFoto fotoDePool(Producto producto, Long id, Long varianteId, String url, int orden) {
        ProductoFoto foto = new ProductoFoto();
        foto.setId(id);
        foto.setProducto(producto);
        foto.setVarianteId(varianteId);
        foto.setUrl(url);
        foto.setOrden(orden);
        return foto;
    }

    // --- listar()/obtener() ---

    @Test
    void listarDevuelveSoloProductosActivosMapeadosADto() {
        Producto p1 = productoConId(1L);
        p1.setNombre("Mate");
        p1.setCategoria(categoriaValida());
        p1.setPrecioVenta(new BigDecimal("100.00"));
        when(productoRepository.findByEmpresaIdAndActivoTrue(EMPRESA_ID)).thenReturn(List.of(p1));

        List<ProductoDto> resultado = service.listar(principal);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Mate");
    }

    @Test
    void listarSinEmpresaEnElPrincipalLanzaExcepcion() {
        UserPrincipal superAdmin = new UserPrincipal(1L, null, true, null);

        assertThatThrownBy(() -> service.listar(superAdmin))
                .isInstanceOf(SinEmpresaException.class);
    }

    @Test
    void obtenerDevuelveElProductoMapeadoADto() {
        Producto producto = productoConId(5L);
        producto.setNombre("Termo");
        producto.setCategoria(categoriaValida());
        producto.setPrecioVenta(new BigDecimal("200.00"));
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        ProductoDto resultado = service.obtener(5L, principal);

        assertThat(resultado.nombre()).isEqualTo("Termo");
    }

    @Test
    void obtenerDeProductoInexistenteLanzaExcepcion() {
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(5L, principal))
                .isInstanceOf(ProductoNoEncontradoException.class);
    }

    // --- eliminar(): soft delete, no borra la fila ---

    @Test
    void eliminarMarcaElProductoComoInactivoSinBorrarLaFila() {
        Producto producto = productoConId(5L);
        producto.setActivo(true);
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        service.eliminar(5L, principal);

        assertThat(producto.isActivo()).isFalse();
        verify(productoRepository).save(producto);
    }

    @Test
    void agregarFotoConFalloDeIOLanzaUncheckedIOException() throws IOException {
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(imagenUploadValidator.validarYObtenerExtension(any())).thenReturn(".jpg");
        MultipartFile file = mock(MultipartFile.class);
        doThrow(new IOException("disco lleno")).when(file).transferTo(any(Path.class));

        assertThatThrownBy(() -> service.agregarFoto(5L, file, null, principal))
                .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void eliminarFotoConUrlPropiaIntentaBorrarElArchivoDelDisco() {
        // La URL matchea el prefijo /uploads/productos/, dispara
        // Files.deleteIfExists (best-effort: el archivo no existe realmente
        // en disco y no debe explotar).
        Producto producto = productoConId(5L);
        producto.setCategoria(categoriaValida());
        ProductoFoto foto = fotoDePool(producto, 1L, null, "/uploads/productos/no-existe-" + UUID.randomUUID() + ".jpg", 0);
        producto.getFotos().add(foto);
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));

        ProductoDto resultado = service.eliminarFoto(5L, 1L, principal);

        assertThat(producto.getFotos()).isEmpty();
        assertThat(resultado.fotoUrl()).isNull();
    }

    // --- subirFotoVariante ---

    @Test
    void subirFotoVarianteGuardaElArchivoYDevuelveLaUrl() {
        when(imagenUploadValidator.validarYObtenerExtension(any())).thenReturn(".png");

        FotoUploadDto resultado = service.subirFotoVariante(mock(MultipartFile.class), principal);

        assertThat(resultado.fotoUrl()).startsWith("/uploads/productos/").endsWith(".png");
    }

    @Test
    void subirFotoVarianteSinEmpresaLanzaExcepcion() {
        UserPrincipal superAdmin = new UserPrincipal(1L, null, true, null);

        assertThatThrownBy(() -> service.subirFotoVariante(mock(MultipartFile.class), superAdmin))
                .isInstanceOf(SinEmpresaException.class);
    }

    // --- eliminarResena / actualizarFotoResena ---

    @Test
    void eliminarResenaBorraLaFilaDelRepositorio() {
        Producto producto = productoConId(5L);
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        Resena resena = new Resena();
        resena.setId(10L);
        when(resenaRepository.findByIdAndProductoIdAndEmpresaId(10L, 5L, EMPRESA_ID)).thenReturn(Optional.of(resena));

        service.eliminarResena(5L, 10L, principal);

        verify(resenaRepository).delete(resena);
    }

    @Test
    void eliminarResenaInexistenteLanzaExcepcion() {
        Producto producto = productoConId(5L);
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        when(resenaRepository.findByIdAndProductoIdAndEmpresaId(10L, 5L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminarResena(5L, 10L, principal))
                .isInstanceOf(ResenaNoEncontradaException.class);
    }

    @Test
    void actualizarFotoResenaGuardaLaImagenYDevuelveLaUrl() {
        Producto producto = productoConId(5L);
        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(producto));
        Resena resena = new Resena();
        resena.setId(10L);
        resena.setClienteNombre("Juana");
        when(resenaRepository.findByIdAndProductoIdAndEmpresaId(10L, 5L, EMPRESA_ID)).thenReturn(Optional.of(resena));
        when(resenaRepository.save(any(Resena.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imagenUploadValidator.validarYObtenerExtension(any())).thenReturn(".jpg");

        ResenaDto resultado = service.actualizarFotoResena(5L, 10L, mock(MultipartFile.class), principal);

        assertThat(resultado.imagenUrl()).startsWith("/uploads/resenas/");
        assertThat(resena.getImagenUrl()).isEqualTo(resultado.imagenUrl());
    }

    // --- subcategoría ---

    @Test
    void crearConSubcategoriaValidaLaAsociaAlProducto() {
        Subcategoria subcategoria = new Subcategoria();
        subcategoria.setId(50L);
        subcategoria.setCategoriaId(CATEGORIA_ID);
        subcategoria.setNombre("Mates de madera");
        when(subcategoriaRepository.findByIdAndCategoriaId(50L, CATEGORIA_ID)).thenReturn(Optional.of(subcategoria));

        ProductoRequest request = new ProductoRequest("Mate", CATEGORIA_ID, 50L, null,
                new BigDecimal("100.00"), null, null, null, null, null, null, null, null, null);

        ProductoDto dto = service.crear(request, principal);

        assertThat(dto.subcategoria()).isEqualTo("Mates de madera");
    }

    @Test
    void crearConSubcategoriaInexistenteLanzaExcepcion() {
        when(subcategoriaRepository.findByIdAndCategoriaId(50L, CATEGORIA_ID)).thenReturn(Optional.empty());

        ProductoRequest request = new ProductoRequest("Mate", CATEGORIA_ID, 50L, null,
                new BigDecimal("100.00"), null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.crear(request, principal))
                .isInstanceOf(SubcategoriaNoEncontradaException.class);
    }

    // --- grabados ---

    @Test
    void crearConGrabadosLosPersisteYLosMapeaEnElDto() {
        ProductoRequest request = new ProductoRequest("Mate", CATEGORIA_ID, null, null,
                new BigDecimal("100.00"), null, null, null, null, null,
                List.of(new ProductoGrabadoRequest("Base", new BigDecimal("500.00"))),
                null, null, null);

        ProductoDto dto = service.crear(request, principal);

        assertThat(dto.grabados())
                .extracting(ProductoGrabadoDto::lugar, ProductoGrabadoDto::precio)
                .containsExactly(tuple("Base", new BigDecimal("500.00")));
    }

    @Test
    void crearConGrabadosNullDejaProductoSinGrabados() {
        ProductoRequest request = new ProductoRequest("Mate", CATEGORIA_ID, null, null,
                new BigDecimal("100.00"), null, null, null, null, null, null, null, null, null);

        ProductoDto dto = service.crear(request, principal);

        assertThat(dto.grabados()).isEmpty();
    }

    @Test
    void actualizarGrabadosReemplazaListaCompleta() {
        // A diferencia de variantes (merge por id), aplicarGrabados hace
        // clear() + recrea todo desde cero — no hay "actualizar in-place" de
        // un grabado existente, ver ProductoServiceImpl.aplicarGrabados.
        Producto existente = productoConId(5L);
        existente.getGrabados().add(grabado(existente, 200L, "Virola", new BigDecimal("100.00")));

        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));

        ActualizarProductoRequest request = new ActualizarProductoRequest("Mate", CATEGORIA_ID, null, null,
                new BigDecimal("100.00"), null, null, null, null, null,
                List.of(new ProductoGrabadoRequest("Base", new BigDecimal("500.00"))),
                null, null, null);

        ProductoDto dto = service.actualizar(5L, request, principal);

        assertThat(dto.grabados())
                .extracting(ProductoGrabadoDto::lugar, ProductoGrabadoDto::precio)
                .containsExactly(tuple("Base", new BigDecimal("500.00")));
    }

    @Test
    void actualizarConGrabadosNullDejaProductoSinGrabados() {
        Producto existente = productoConId(5L);
        existente.getGrabados().add(grabado(existente, 200L, "Virola", new BigDecimal("100.00")));

        when(productoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));

        ActualizarProductoRequest request = new ActualizarProductoRequest("Mate", CATEGORIA_ID, null, null,
                new BigDecimal("100.00"), null, null, null, null, null, null, null, null, null);

        ProductoDto dto = service.actualizar(5L, request, principal);

        assertThat(dto.grabados()).isEmpty();
    }

    // --- Helpers ---

    private Producto productoConId(Long id) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setStock(0);
        return producto;
    }

    private Categoria categoriaValida() {
        Categoria categoria = new Categoria();
        categoria.setId(CATEGORIA_ID);
        categoria.setNombre("General");
        return categoria;
    }

    private ProductoRequest requestConComponentes(List<ProductoComponenteRequest> componentes) {
        return new ProductoRequest("Kit", CATEGORIA_ID, null, null, new BigDecimal("100.00"), null, null,
                null, null, componentes, null, null, null, null);
    }

    private ActualizarProductoRequest requestActualizarConComponentes(List<ProductoComponenteRequest> componentes) {
        return new ActualizarProductoRequest("Kit", CATEGORIA_ID, null, null, new BigDecimal("100.00"), null, null,
                null, null, componentes, null, null, null, null);
    }

    private ProductoGrabado grabado(Producto producto, Long id, String lugar, BigDecimal precio) {
        ProductoGrabado grabado = new ProductoGrabado();
        grabado.setId(id);
        grabado.setProducto(producto);
        grabado.setLugar(lugar);
        grabado.setPrecio(precio);
        return grabado;
    }
}
