package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ActualizarEstadoVentaRequest;
import com.sistventas.backend.dto.FotoUploadDto;
import com.sistventas.backend.dto.TextoCompartirDto;
import com.sistventas.backend.dto.VentaDto;
import com.sistventas.backend.dto.VentaEstadoHistorialDto;
import com.sistventas.backend.dto.VentaRequest;
import com.sistventas.backend.entity.EstadoVenta;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
 * Flujo unificado de Presupuesto+Venta de la empresa del usuario logueado.
 * Todo el scoping multiempresa se resuelve vía UserPrincipal
 * (@AuthenticationPrincipal) — ningún método acá confía en un empresaId del
 * body/query del cliente.
 */
@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @GetMapping
    public ResponseEntity<List<VentaDto>> listar(
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) Long clienteId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ventaService.listar(estado, clienteId, principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaDto> obtener(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ventaService.obtener(id, principal));
    }

    @PostMapping
    public ResponseEntity<VentaDto> crear(
            @Valid @RequestBody VentaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        VentaDto dto = ventaService.crear(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VentaDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody VentaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ventaService.actualizar(id, request, principal));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<VentaDto> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoVentaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ventaService.actualizarEstado(id, request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ventaService.eliminar(id, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/historial-estados")
    public ResponseEntity<List<VentaEstadoHistorialDto>> historialEstados(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ventaService.historialEstados(id, principal));
    }

    @GetMapping("/{id}/texto-compartir")
    public ResponseEntity<TextoCompartirDto> textoCompartir(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ventaService.textoCompartir(id, principal));
    }

    // Upload genérico, no atado a un item/venta: ver VentaService.subirFoto.
    // El frontend sube la foto mientras arma el formulario (venta nueva o
    // existente) y guarda la fotoUrl devuelta en el item correspondiente,
    // para mandarla como parte del payload normal de POST/PUT /api/ventas.
    @PostMapping(value = "/fotos", consumes = "multipart/form-data")
    public ResponseEntity<FotoUploadDto> subirFoto(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ventaService.subirFoto(file, principal));
    }
}
