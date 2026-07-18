package com.sistventas.backend.controller;

import com.sistventas.backend.dto.CategoriaTiendaDto;
import com.sistventas.backend.dto.PreviewDescuentoComboDto;
import com.sistventas.backend.dto.PreviewDescuentoComboRequest;
import com.sistventas.backend.dto.PublicEmpresaDto;
import com.sistventas.backend.dto.PublicPedidoEstadoDto;
import com.sistventas.backend.dto.PublicPedidoRequest;
import com.sistventas.backend.dto.PublicPedidoResultadoDto;
import com.sistventas.backend.dto.PublicProductoDto;
import com.sistventas.backend.dto.PublicTestimonioDto;
import com.sistventas.backend.dto.PublicTipDto;
import com.sistventas.backend.dto.ResenaDto;
import com.sistventas.backend.service.PublicTiendaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/{slug}/productos")
    public ResponseEntity<List<PublicProductoDto>> listarProductos(@PathVariable String slug) {
        return ResponseEntity.ok(publicTiendaService.listarProductos(slug));
    }

    @GetMapping("/{slug}/categorias")
    public ResponseEntity<List<CategoriaTiendaDto>> listarCategorias(@PathVariable String slug) {
        return ResponseEntity.ok(publicTiendaService.listarCategorias(slug));
    }

    @GetMapping("/{slug}/productos/{productoId}/resenas")
    public ResponseEntity<List<ResenaDto>> listarResenas(
            @PathVariable String slug,
            @PathVariable Long productoId) {
        return ResponseEntity.ok(publicTiendaService.listarResenas(slug, productoId));
    }

    @GetMapping("/{slug}/testimonios")
    public ResponseEntity<List<PublicTestimonioDto>> listarTestimonios(@PathVariable String slug) {
        return ResponseEntity.ok(publicTiendaService.listarTestimonios(slug));
    }

    @GetMapping("/{slug}/tips")
    public ResponseEntity<List<PublicTipDto>> listarTips(@PathVariable String slug) {
        return ResponseEntity.ok(publicTiendaService.listarTips(slug));
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
