package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ActualizarActivoRequest;
import com.sistventas.backend.dto.InvitarUsuarioRequest;
import com.sistventas.backend.dto.UsuarioEmpresaDto;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.UsuarioEmpresaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestión de usuarios dentro de la empresa del ADMIN logueado. Restringido a
 * rolEmpresa=ADMIN — el chequeo vive en el service (mismo criterio que el
 * resto del proyecto: la SecurityConfig no distingue roles, todo /api/**
 * requiere solo estar autenticado).
 */
@RestController
@RequestMapping("/api/empresa/usuarios")
public class UsuarioEmpresaController {

    private final UsuarioEmpresaService usuarioEmpresaService;

    public UsuarioEmpresaController(UsuarioEmpresaService usuarioEmpresaService) {
        this.usuarioEmpresaService = usuarioEmpresaService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioEmpresaDto>> listar(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(usuarioEmpresaService.listar(principal));
    }

    @PostMapping
    public ResponseEntity<UsuarioEmpresaDto> invitar(
            @Valid @RequestBody InvitarUsuarioRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UsuarioEmpresaDto dto = usuarioEmpresaService.invitar(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PatchMapping("/{id}/activo")
    public ResponseEntity<UsuarioEmpresaDto> actualizarActivo(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarActivoRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(usuarioEmpresaService.actualizarActivo(id, request, principal));
    }
}
