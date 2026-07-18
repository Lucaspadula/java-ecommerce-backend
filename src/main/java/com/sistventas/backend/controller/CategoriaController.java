package com.sistventas.backend.controller;

import com.sistventas.backend.dto.CategoriaDto;
import com.sistventas.backend.dto.CrearCategoriaRequest;
import com.sistventas.backend.dto.CrearSubcategoriaRequest;
import com.sistventas.backend.dto.SubcategoriaDto;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogo maestro de categorías/subcategorías de producto de la empresa del
 * usuario logueado. Mismo scoping multiempresa que ProductoController: el
 * empresaId siempre sale del UserPrincipal, nunca de un valor del cliente.
 */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public ResponseEntity<List<CategoriaDto>> listar(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(categoriaService.listar(principal));
    }

    @PostMapping
    public ResponseEntity<CategoriaDto> crear(
            @Valid @RequestBody CrearCategoriaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        CategoriaDto dto = categoriaService.crear(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{categoriaId}/subcategorias")
    public ResponseEntity<List<SubcategoriaDto>> listarSubcategorias(
            @PathVariable Long categoriaId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(categoriaService.listarSubcategorias(categoriaId, principal));
    }

    @PostMapping("/{categoriaId}/subcategorias")
    public ResponseEntity<SubcategoriaDto> crearSubcategoria(
            @PathVariable Long categoriaId,
            @Valid @RequestBody CrearSubcategoriaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        SubcategoriaDto dto = categoriaService.crearSubcategoria(categoriaId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
