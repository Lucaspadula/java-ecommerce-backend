package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ProveedorDto;
import com.sistventas.backend.dto.ProveedorRequest;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.ProveedorService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cartera de proveedores de la empresa del usuario logueado. Todo el scoping
 * multiempresa se resuelve vía UserPrincipal (@AuthenticationPrincipal) —
 * ningún método acá confía en un empresaId del body/query del cliente.
 */
@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public ResponseEntity<List<ProveedorDto>> listar(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(proveedorService.listar(principal));
    }

    @GetMapping("/inactivos")
    public ResponseEntity<List<ProveedorDto>> listarInactivos(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(proveedorService.listarInactivos(principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorDto> obtener(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(proveedorService.obtener(id, principal));
    }

    @PostMapping
    public ResponseEntity<ProveedorDto> crear(
            @Valid @RequestBody ProveedorRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ProveedorDto dto = proveedorService.crear(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProveedorDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProveedorRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(proveedorService.actualizar(id, request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        proveedorService.eliminar(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restaurar")
    public ResponseEntity<ProveedorDto> restaurar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(proveedorService.restaurar(id, principal));
    }
}
