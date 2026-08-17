package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ImportarInsumosResultadoDto;
import com.sistventas.backend.dto.InsumoDto;
import com.sistventas.backend.dto.InsumoRequest;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.InsumoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
 * Insumos maestros de la empresa del usuario logueado (Etapa 1 de
 * inventario). Todo el scoping multiempresa se resuelve vía UserPrincipal
 * (@AuthenticationPrincipal) — ningún método acá confía en un empresaId del
 * body/query del cliente.
 */
@RestController
@RequestMapping("/api/insumos")
public class InsumoController {

    private final InsumoService insumoService;

    public InsumoController(InsumoService insumoService) {
        this.insumoService = insumoService;
    }

    @GetMapping
    public ResponseEntity<List<InsumoDto>> listar(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(insumoService.listar(principal));
    }

    @GetMapping("/inactivos")
    public ResponseEntity<List<InsumoDto>> listarInactivos(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(insumoService.listarInactivos(principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsumoDto> obtener(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(insumoService.obtener(id, principal));
    }

    @PostMapping
    public ResponseEntity<InsumoDto> crear(
            @Valid @RequestBody InsumoRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        InsumoDto dto = insumoService.crear(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InsumoDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody InsumoRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(insumoService.actualizar(id, request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        insumoService.eliminar(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restaurar")
    public ResponseEntity<InsumoDto> restaurar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(insumoService.restaurar(id, principal));
    }

    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> plantilla() {
        byte[] excel = insumoService.generarPlantilla();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla-insumos.xlsx\"")
                .body(excel);
    }

    @PostMapping(value = "/importar", consumes = "multipart/form-data")
    public ResponseEntity<ImportarInsumosResultadoDto> importar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(insumoService.importarDesdeExcel(file, principal));
    }
}
