package com.sistventas.backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Un test por cada @ExceptionHandler de GlobalExceptionHandler. Es una clase
// POJO (@RestControllerAdvice, pero sin dependencias) así que no hace falta
// levantar Spring: se invoca cada método directo con la excepción y se
// verifica el status HTTP + el body Map.of("error", mensaje) que espera el
// frontend.
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void emailYaRegistradoDevuelve409ConMensaje() {
        ResponseEntity<Map<String, String>> response =
                handler.handleEmailYaRegistrado(new EmailYaRegistradoException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Ese email ya está registrado"));
    }

    @Test
    void credencialesInvalidasDevuelve401() {
        ResponseEntity<Map<String, String>> response =
                handler.handleCredencialesInvalidas(new CredencialesInvalidasException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Credenciales inválidas"));
    }

    @Test
    void tokenGoogleInvalidoDevuelve401() {
        ResponseEntity<Map<String, String>> response =
                handler.handleTokenGoogleInvalido(new TokenGoogleInvalidoException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "El token de Google no es válido"));
    }

    @Test
    void cuentaDeshabilitadaDevuelve403() {
        ResponseEntity<Map<String, String>> response =
                handler.handleCuentaDeshabilitada(new CuentaDeshabilitadaException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody())
                .containsExactly(Map.entry("error", "Tu cuenta está deshabilitada. Contactá al administrador."));
    }

    @Test
    void licenciaNoActivaDevuelve403ConMensajePropio() {
        ResponseEntity<Map<String, String>> response =
                handler.handleLicenciaNoActiva(new LicenciaNoActivaException("La licencia venció"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "La licencia venció"));
    }

    @Test
    void empresaNoEncontradaDevuelve404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleEmpresaNoEncontrada(new EmpresaNoEncontradaException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Empresa no encontrada"));
    }

    @Test
    void productoNoEncontradoDevuelve404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleProductoNoEncontrado(new ProductoNoEncontradoException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Producto no encontrado"));
    }

    @Test
    void sinEmpresaDevuelve403() {
        ResponseEntity<Map<String, String>> response =
                handler.handleSinEmpresa(new SinEmpresaException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody())
                .containsExactly(Map.entry("error", "El usuario no pertenece a ninguna empresa"));
    }

    @Test
    void clienteNoEncontradoDevuelve404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleClienteNoEncontrado(new ClienteNoEncontradoException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Cliente no encontrado"));
    }

    @Test
    void proveedorNoEncontradoDevuelve404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleProveedorNoEncontrado(new ProveedorNoEncontradoException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Proveedor no encontrado"));
    }

    @Test
    void insumoNoEncontradoDevuelve404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleInsumoNoEncontrado(new InsumoNoEncontradoException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Insumo no encontrado"));
    }

    @Test
    void categoriaNoEncontradaDevuelve404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleCategoriaNoEncontrada(new CategoriaNoEncontradaException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Categoría no encontrada"));
    }

    @Test
    void subcategoriaNoEncontradaDevuelve404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleSubcategoriaNoEncontrada(new SubcategoriaNoEncontradaException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Subcategoría no encontrada"));
    }

    @Test
    void ventaNoEncontradaDevuelve404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleVentaNoEncontrada(new VentaNoEncontradaException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Venta no encontrada"));
    }

    @Test
    void archivoInvalidoDevuelve400ConMensajePropio() {
        ResponseEntity<Map<String, String>> response =
                handler.handleArchivoInvalido(new ArchivoInvalidoException("El archivo supera el tamaño máximo"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsExactly(Map.entry("error", "El archivo supera el tamaño máximo"));
    }

    @Test
    void passwordActualIncorrectaDevuelve401() {
        ResponseEntity<Map<String, String>> response =
                handler.handlePasswordActualIncorrecta(new PasswordActualIncorrectaException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody())
                .containsExactly(Map.entry("error", "La contraseña actual es incorrecta"));
    }

    @Test
    void accesoRestringidoAdminDevuelve403() {
        ResponseEntity<Map<String, String>> response =
                handler.handleAccesoRestringidoAdmin(new AccesoRestringidoAdminException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsExactly(
                Map.entry("error", "Solo el administrador de la empresa puede realizar esta acción"));
    }

    @Test
    void usuarioNoEncontradoDevuelve404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleUsuarioNoEncontrado(new UsuarioNoEncontradoException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Usuario no encontrado"));
    }

    @Test
    void accionNoPermitidaDevuelve400ConMensajePropio() {
        ResponseEntity<Map<String, String>> response =
                handler.handleAccionNoPermitida(new AccionNoPermitidaException("Ya existe un cliente con ese nombre"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsExactly(Map.entry("error", "Ya existe un cliente con ese nombre"));
    }

    @Test
    void stockInsuficienteDevuelve409ConMensajeArmado() {
        ResponseEntity<Map<String, String>> response =
                handler.handleStockInsuficiente(new StockInsuficienteException("Remera", 2, 5));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsExactly(
                Map.entry("error", "Stock insuficiente para \"Remera\": disponible 2, solicitado 5"));
    }

    @Test
    void ajusteStockInvalidoDevuelve409ConMensajeArmado() {
        ResponseEntity<Map<String, String>> response =
                handler.handleAjusteStockInvalido(new AjusteStockInvalidoException("Remera", 3, -5));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsExactly(Map.entry("error",
                "El ajuste dejaría el stock en negativo para \"Remera\": actual 3, delta -5"));
    }

    @Test
    void slugEnUsoDevuelve409() {
        ResponseEntity<Map<String, String>> response =
                handler.handleSlugEnUso(new SlugEnUsoException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .containsExactly(Map.entry("error", "Ese identificador de tienda ya está en uso"));
    }

    @Test
    void tiendaNoEncontradaDevuelve404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleTiendaNoEncontrada(new TiendaNoEncontradaException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Tienda no encontrada"));
    }

    @Test
    void cuponInvalidoDevuelve400() {
        ResponseEntity<Map<String, String>> response =
                handler.handleCuponInvalido(new CuponInvalidoException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsExactly(Map.entry("error", "El código de descuento no es válido"));
    }

    @Test
    void resenaNoEncontradaDevuelve404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleResenaNoEncontrada(new ResenaNoEncontradaException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Reseña no encontrada"));
    }

    @Test
    void bannerImagenNoEncontradaDevuelve404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleBannerImagenNoEncontrada(new BannerImagenNoEncontradaException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Imagen de banner no encontrada"));
    }

    @Test
    void testimonioNoEncontradoDevuelve404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleTestimonioNoEncontrado(new TestimonioNoEncontradoException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Testimonio no encontrado"));
    }

    @Test
    void tipNoEncontradoDevuelve404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleTipNoEncontrado(new TipNoEncontradoException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Tip no encontrado"));
    }

    @Test
    void reglaDescuentoComboNoEncontradaDevuelve404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleReglaDescuentoComboNoEncontrada(new ReglaDescuentoComboNoEncontradaException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Regla de descuento no encontrada"));
    }

    @Test
    void reglaDescuentoComboInvalidaDevuelve400() {
        ResponseEntity<Map<String, String>> response =
                handler.handleReglaDescuentoComboInvalida(new ReglaDescuentoComboInvalidaException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsExactly(Map.entry("error", "Los dos lados de la regla deben ser distintos"));
    }

    @Test
    void productoComponenteInvalidoDevuelve400ConMensajePropio() {
        ResponseEntity<Map<String, String>> response = handler.handleProductoComponenteInvalido(
                new ProductoComponenteInvalidoException("Un producto no puede tenerse a sí mismo como componente"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsExactly(
                Map.entry("error", "Un producto no puede tenerse a sí mismo como componente"));
    }

    @Test
    void validationConErroresDeCampoDevuelve400ConElPrimerMensaje() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError primerError = new FieldError("request", "nombre", "El nombre es obligatorio");
        FieldError segundoError = new FieldError("request", "email", "El email es inválido");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(primerError, segundoError));

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "El nombre es obligatorio"));
    }

    @Test
    void validationSinErroresDeCampoDevuelve400ConMensajeGenerico() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Collections.emptyList());

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Datos inválidos"));
    }
}
