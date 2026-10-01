package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ActualizarActivoRequest;
import com.sistventas.backend.dto.BloqueImagenUploadDto;
import com.sistventas.backend.dto.GuardarTiendaBloqueCardRequest;
import com.sistventas.backend.dto.GuardarTiendaBloqueRequest;
import com.sistventas.backend.dto.ReordenarBloquesRequest;
import com.sistventas.backend.dto.ReordenarCardsRequest;
import com.sistventas.backend.dto.TiendaBloqueCardDto;
import com.sistventas.backend.dto.TiendaBloqueDto;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.TiendaBloqueService;
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
 * Bloques configurables de la tienda pública. Restringido a rolEmpresa=ADMIN:
 * el chequeo vive en el service (la SecurityConfig solo exige estar
 * autenticado en /api/**, mismo criterio que EmpresaController y
 * UsuarioEmpresaController).
 */
@RestController
@RequestMapping("/api/empresa/tienda/bloques")
public class TiendaBloqueController {

    private final TiendaBloqueService tiendaBloqueService;

    public TiendaBloqueController(TiendaBloqueService tiendaBloqueService) {
        this.tiendaBloqueService = tiendaBloqueService;
    }

    @GetMapping
    public ResponseEntity<List<TiendaBloqueDto>> listar(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tiendaBloqueService.listar(principal));
    }

    @PostMapping
    public ResponseEntity<TiendaBloqueDto> crear(
            @Valid @RequestBody GuardarTiendaBloqueRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tiendaBloqueService.crear(request, principal));
    }

    // Las rutas literales (/orden, /imagenes) ganan sobre /{id} en Spring.
    @PutMapping("/orden")
    public ResponseEntity<List<TiendaBloqueDto>> reordenar(
            @RequestBody ReordenarBloquesRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tiendaBloqueService.reordenar(request, principal));
    }

    @PostMapping(value = "/imagenes", consumes = "multipart/form-data")
    public ResponseEntity<BloqueImagenUploadDto> subirImagen(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tiendaBloqueService.subirImagen(file, principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TiendaBloqueDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody GuardarTiendaBloqueRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tiendaBloqueService.actualizar(id, request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        tiendaBloqueService.eliminar(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activo")
    public ResponseEntity<TiendaBloqueDto> actualizarActivo(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarActivoRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tiendaBloqueService.actualizarActivo(id, request.activo(), principal));
    }

    @PostMapping("/{id}/cards")
    public ResponseEntity<TiendaBloqueCardDto> crearCard(
            @PathVariable Long id,
            @Valid @RequestBody GuardarTiendaBloqueCardRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tiendaBloqueService.crearCard(id, request, principal));
    }

    @PutMapping("/{id}/cards/orden")
    public ResponseEntity<List<TiendaBloqueCardDto>> reordenarCards(
            @PathVariable Long id,
            @RequestBody ReordenarCardsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tiendaBloqueService.reordenarCards(id, request, principal));
    }

    @PutMapping("/{id}/cards/{cardId}")
    public ResponseEntity<TiendaBloqueCardDto> actualizarCard(
            @PathVariable Long id,
            @PathVariable Long cardId,
            @Valid @RequestBody GuardarTiendaBloqueCardRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tiendaBloqueService.actualizarCard(id, cardId, request, principal));
    }

    @DeleteMapping("/{id}/cards/{cardId}")
    public ResponseEntity<Void> eliminarCard(
            @PathVariable Long id,
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserPrincipal principal) {
        tiendaBloqueService.eliminarCard(id, cardId, principal);
        return ResponseEntity.noContent().build();
    }
}
