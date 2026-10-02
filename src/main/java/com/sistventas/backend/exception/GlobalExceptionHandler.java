package com.sistventas.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Centraliza el mapeo de excepciones de auth a las respuestas JSON exactas
 * que espera el frontend. Mantiene el controller y el service libres de
 * lógica de manejo de errores HTTP.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailYaRegistradoException.class)
    public ResponseEntity<Map<String, String>> handleEmailYaRegistrado(EmailYaRegistradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, String>> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(TokenGoogleInvalidoException.class)
    public ResponseEntity<Map<String, String>> handleTokenGoogleInvalido(TokenGoogleInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(CuentaDeshabilitadaException.class)
    public ResponseEntity<Map<String, String>> handleCuentaDeshabilitada(CuentaDeshabilitadaException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(LicenciaNoActivaException.class)
    public ResponseEntity<Map<String, String>> handleLicenciaNoActiva(LicenciaNoActivaException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(EmpresaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleEmpresaNoEncontrada(EmpresaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ProductoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleProductoNoEncontrado(ProductoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SinEmpresaException.class)
    public ResponseEntity<Map<String, String>> handleSinEmpresa(SinEmpresaException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleClienteNoEncontrado(ClienteNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ProveedorNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleProveedorNoEncontrado(ProveedorNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InsumoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleInsumoNoEncontrado(InsumoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(CategoriaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleCategoriaNoEncontrada(CategoriaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SubcategoriaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleSubcategoriaNoEncontrada(SubcategoriaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(VentaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleVentaNoEncontrada(VentaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ArchivoInvalidoException.class)
    public ResponseEntity<Map<String, String>> handleArchivoInvalido(ArchivoInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(PasswordActualIncorrectaException.class)
    public ResponseEntity<Map<String, String>> handlePasswordActualIncorrecta(PasswordActualIncorrectaException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AccesoRestringidoAdminException.class)
    public ResponseEntity<Map<String, String>> handleAccesoRestringidoAdmin(AccesoRestringidoAdminException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AccionNoPermitidaException.class)
    public ResponseEntity<Map<String, String>> handleAccionNoPermitida(AccionNoPermitidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<Map<String, String>> handleStockInsuficiente(StockInsuficienteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AjusteStockInvalidoException.class)
    public ResponseEntity<Map<String, String>> handleAjusteStockInvalido(AjusteStockInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SlugEnUsoException.class)
    public ResponseEntity<Map<String, String>> handleSlugEnUso(SlugEnUsoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(TiendaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleTiendaNoEncontrada(TiendaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(CuponInvalidoException.class)
    public ResponseEntity<Map<String, String>> handleCuponInvalido(CuponInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AtributoFiltroNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleAtributoFiltroNoEncontrado(AtributoFiltroNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AtributoFiltroValorNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleAtributoFiltroValorNoEncontrado(AtributoFiltroValorNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ClienteYaRegistradoException.class)
    public ResponseEntity<Map<String, String>> handleClienteYaRegistrado(ClienteYaRegistradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ResenaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleResenaNoEncontrada(ResenaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ProductoFotoNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleProductoFotoNoEncontrada(ProductoFotoNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(BannerImagenNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleBannerImagenNoEncontrada(BannerImagenNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(BloqueTiendaInvalidoException.class)
    public ResponseEntity<Map<String, String>> handleBloqueTiendaInvalido(BloqueTiendaInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(BloqueTiendaNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleBloqueTiendaNoEncontrado(BloqueTiendaNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(CatalogoSeccionNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleCatalogoSeccionNoEncontrada(CatalogoSeccionNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ReglaDescuentoComboNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleReglaDescuentoComboNoEncontrada(ReglaDescuentoComboNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ReglaDescuentoComboInvalidaException.class)
    public ResponseEntity<Map<String, String>> handleReglaDescuentoComboInvalida(ReglaDescuentoComboInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ProductoComponenteInvalidoException.class)
    public ResponseEntity<Map<String, String>> handleProductoComponenteInvalido(ProductoComponenteInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IaNoConfiguradaException.class)
    public ResponseEntity<Map<String, String>> handleIaNoConfigurada(IaNoConfiguradaException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IaGeneracionFallidaException.class)
    public ResponseEntity<Map<String, String>> handleIaGeneracionFallida(IaGeneracionFallidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("Datos inválidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", mensaje));
    }
}
