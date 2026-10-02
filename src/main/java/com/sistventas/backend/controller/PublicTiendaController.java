package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ActualizarPerfilClienteRequest;
import com.sistventas.backend.dto.AtributoFiltroDto;
import com.sistventas.backend.dto.CategoriaTiendaDto;
import com.sistventas.backend.dto.ClienteLoginDto;
import com.sistventas.backend.dto.ClienteLoginGoogleRequest;
import com.sistventas.backend.dto.ClienteLoginRequest;
import com.sistventas.backend.dto.ClienteLoginResponse;
import com.sistventas.backend.dto.CrearResenaClienteRequest;
import com.sistventas.backend.dto.RegistrarClienteGoogleRequest;
import com.sistventas.backend.dto.RegistrarClienteRequest;
import com.sistventas.backend.dto.FotoUploadDto;
import com.sistventas.backend.dto.PreviewDescuentoComboDto;
import com.sistventas.backend.dto.PreviewDescuentoComboRequest;
import com.sistventas.backend.dto.PublicCategoriaMenuDto;
import com.sistventas.backend.dto.PublicEmpresaDto;
import com.sistventas.backend.dto.PublicPedidoEstadoDto;
import com.sistventas.backend.dto.PublicPedidoRequest;
import com.sistventas.backend.dto.PublicPedidoResultadoDto;
import com.sistventas.backend.dto.PublicProductoDto;
import com.sistventas.backend.dto.ResenaDto;
import com.sistventas.backend.exception.CredencialesInvalidasException;
import com.sistventas.backend.security.ClientePrincipal;
import com.sistventas.backend.service.PublicTiendaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Vidriera pública, sin login: /api/public/** es permitAll en SecurityConfig.
 * Ningún método recibe @AuthenticationPrincipal, la empresa se resuelve
 * siempre por el slug de la URL.
 */
@RestController
@RequestMapping("/api/public/tienda")
public class PublicTiendaController {

    private final PublicTiendaService publicTiendaService;

    public PublicTiendaController(PublicTiendaService publicTiendaService) {
        this.publicTiendaService = publicTiendaService;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<PublicEmpresaDto> obtenerEmpresa(@PathVariable String slug) {
        return ResponseEntity.ok(publicTiendaService.obtenerEmpresa(slug));
    }

    // Subida ANÓNIMA (sin JWT, ver clase) del logo/diseño del grabado —
    // rate-limiteada en RateLimitFilter (GRABADO_FOTO), no es gratis dejar
    // esto sin límite en una ruta pública sin fricción.
    @PostMapping(value = "/{slug}/grabado/foto", consumes = "multipart/form-data")
    public ResponseEntity<FotoUploadDto> subirFotoGrabado(
            @PathVariable String slug,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(publicTiendaService.subirFotoGrabado(slug, file));
    }

    @GetMapping("/{slug}/productos")
    public ResponseEntity<List<PublicProductoDto>> listarProductos(@PathVariable String slug) {
        return ResponseEntity.ok(publicTiendaService.listarProductos(slug));
    }

    @GetMapping("/{slug}/categorias")
    public ResponseEntity<List<CategoriaTiendaDto>> listarCategorias(@PathVariable String slug) {
        return ResponseEntity.ok(publicTiendaService.listarCategorias(slug));
    }

    @GetMapping("/{slug}/categorias-menu")
    public ResponseEntity<List<PublicCategoriaMenuDto>> listarCategoriasMenu(@PathVariable String slug) {
        return ResponseEntity.ok(publicTiendaService.listarCategoriasMenu(slug));
    }

    @GetMapping("/{slug}/categorias/{categoriaId}/atributos")
    public ResponseEntity<List<AtributoFiltroDto>> listarAtributosFiltro(
            @PathVariable String slug,
            @PathVariable Long categoriaId) {
        return ResponseEntity.ok(publicTiendaService.listarAtributosFiltro(slug, categoriaId));
    }

    @GetMapping("/{slug}/productos/{productoId}/resenas")
    public ResponseEntity<List<ResenaDto>> listarResenas(
            @PathVariable String slug,
            @PathVariable Long productoId) {
        return ResponseEntity.ok(publicTiendaService.listarResenas(slug, productoId));
    }

    @PostMapping("/{slug}/productos/{productoId}/resenas")
    public ResponseEntity<ResenaDto> crearResenaCliente(
            @PathVariable String slug,
            @PathVariable Long productoId,
            @Valid @RequestBody CrearResenaClienteRequest request) {
        ResenaDto resena = publicTiendaService.crearResenaCliente(slug, productoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resena);
    }

    @PostMapping("/{slug}/cuenta/login")
    public ResponseEntity<ClienteLoginResponse> loginCliente(
            @PathVariable String slug,
            @Valid @RequestBody ClienteLoginRequest request) {
        return ResponseEntity.ok(publicTiendaService.loginCliente(slug, request));
    }

    @PostMapping("/{slug}/cuenta/login-google")
    public ResponseEntity<ClienteLoginResponse> loginClienteGoogle(
            @PathVariable String slug,
            @Valid @RequestBody ClienteLoginGoogleRequest request) {
        return ResponseEntity.ok(publicTiendaService.loginClienteGoogle(slug, request));
    }

    @PostMapping("/{slug}/cuenta/registro")
    public ResponseEntity<ClienteLoginResponse> registrarCliente(
            @PathVariable String slug,
            @Valid @RequestBody RegistrarClienteRequest request) {
        ClienteLoginResponse respuesta = publicTiendaService.registrarCliente(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PostMapping("/{slug}/cuenta/registro-google")
    public ResponseEntity<ClienteLoginResponse> registrarClienteGoogle(
            @PathVariable String slug,
            @Valid @RequestBody RegistrarClienteGoogleRequest request) {
        ClienteLoginResponse respuesta = publicTiendaService.registrarClienteGoogle(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{slug}/bloques")
    public ResponseEntity<List<com.sistventas.backend.dto.PublicTiendaBloqueDto>> listarBloques(@PathVariable String slug) {
        return ResponseEntity.ok(publicTiendaService.listarBloques(slug));
    }

    @PostMapping("/{slug}/pedidos")
    public ResponseEntity<PublicPedidoResultadoDto> crearPedido(
            @PathVariable String slug,
            @Valid @RequestBody PublicPedidoRequest request) {
        PublicPedidoResultadoDto resultado = publicTiendaService.crearPedido(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @GetMapping("/{slug}/pedidos/{ventaId}")
    public ResponseEntity<PublicPedidoEstadoDto> consultarPedido(
            @PathVariable String slug,
            @PathVariable Long ventaId,
            @RequestParam String telefono) {
        return ResponseEntity.ok(publicTiendaService.consultarPedido(slug, ventaId, telefono));
    }

    // Los 2 endpoints de acá abajo son los únicos de esta clase que SÍ piden
    // @AuthenticationPrincipal (ver comentario de la clase): /api/public/**
    // es permitAll en SecurityConfig, así que un ClientePrincipal ausente no
    // lo bloquea Spring Security solo — si el JWT falta/es inválido/es de
    // Usuario (panel) en vez de Cliente, JwtAuthenticationFilter deja el
    // principal en null y acá se lo trata como no autenticado a mano, mismo
    // CredencialesInvalidasException (401) que el resto de los flujos de
    // cuenta de cliente.
    @GetMapping("/{slug}/cliente/pedidos")
    public ResponseEntity<List<PublicPedidoEstadoDto>> listarPedidosCliente(
            @PathVariable String slug,
            @AuthenticationPrincipal ClientePrincipal principal) {
        if (principal == null) {
            throw new CredencialesInvalidasException();
        }
        return ResponseEntity.ok(publicTiendaService.listarPedidosCliente(slug, principal.clienteId()));
    }

    @PutMapping("/{slug}/cliente/perfil")
    public ResponseEntity<ClienteLoginDto> actualizarPerfilCliente(
            @PathVariable String slug,
            @AuthenticationPrincipal ClientePrincipal principal,
            @Valid @RequestBody ActualizarPerfilClienteRequest request) {
        if (principal == null) {
            throw new CredencialesInvalidasException();
        }
        return ResponseEntity.ok(publicTiendaService.actualizarPerfilCliente(slug, principal.clienteId(), request));
    }

    // Preview informativo para el carrito, antes de confirmar: el monto que
    // termina persistido en la Venta sale de un recálculo server-side
    // idéntico dentro de crearPedido, no de lo que devuelve este endpoint.
    @PostMapping("/{slug}/carrito/descuento-preview")
    public ResponseEntity<PreviewDescuentoComboDto> previewDescuentoCombo(
            @PathVariable String slug,
            @Valid @RequestBody PreviewDescuentoComboRequest request) {
        return ResponseEntity.ok(publicTiendaService.previewDescuentoCombo(slug, request));
    }
}
