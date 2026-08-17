package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ClienteDto;
import com.sistventas.backend.dto.ClienteRequest;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.ClienteService;
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
 * Cartera de clientes de la empresa del usuario logueado. Todo el scoping
 * multiempresa se resuelve vía UserPrincipal (@AuthenticationPrincipal) —
 * ningún método acá confía en un empresaId del body/query del cliente.
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public ResponseEntity<List<ClienteDto>> listar(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(clienteService.listar(principal));
    }

    @GetMapping("/inactivos")
    public ResponseEntity<List<ClienteDto>> listarInactivos(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(clienteService.listarInactivos(principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteDto> obtener(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(clienteService.obtener(id, principal));
    }

    @PostMapping
    public ResponseEntity<ClienteDto> crear(
            @Valid @RequestBody ClienteRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ClienteDto dto = clienteService.crear(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(clienteService.actualizar(id, request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        clienteService.eliminar(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restaurar")
    public ResponseEntity<ClienteDto> restaurar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(clienteService.restaurar(id, principal));
    }
}
