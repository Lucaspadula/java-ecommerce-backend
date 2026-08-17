package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ActualizarGeminiApiKeyRequest;
import com.sistventas.backend.dto.ActualizarPerfilRequest;
import com.sistventas.backend.dto.CambiarPasswordRequest;
import com.sistventas.backend.dto.MensajeResponse;
import com.sistventas.backend.dto.PerfilDto;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Datos del usuario logueado. Disponible para cualquier usuario autenticado,
 * incluido el Super Admin (que no tiene empresa).
 */
@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    public ResponseEntity<PerfilDto> obtener(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(perfilService.obtener(principal));
    }

    @PutMapping
    public ResponseEntity<PerfilDto> actualizar(
            @Valid @RequestBody ActualizarPerfilRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(perfilService.actualizar(request, principal));
    }

    @PutMapping("/password")
    public ResponseEntity<MensajeResponse> cambiarPassword(
            @Valid @RequestBody CambiarPasswordRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(perfilService.cambiarPassword(request, principal));
    }

    // Solo ADMIN (ver PerfilServiceImpl.adminEmpresaIdOrThrow). apiKey vacío
    // = borrar la key propia y volver a usar el fallback global.
    @PutMapping("/gemini-api-key")
    public ResponseEntity<PerfilDto> actualizarGeminiApiKey(
            @RequestBody ActualizarGeminiApiKeyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(perfilService.actualizarGeminiApiKey(request, principal));
    }
}
