package com.sistventas.backend.controller;

import com.sistventas.backend.dto.AtributoFiltroDto;
import com.sistventas.backend.dto.CategoriaDto;
import com.sistventas.backend.dto.CrearAtributoFiltroRequest;
import com.sistventas.backend.dto.CrearAtributoFiltroValorRequest;
import com.sistventas.backend.dto.CrearCategoriaRequest;
import com.sistventas.backend.dto.CrearSubcategoriaRequest;
import com.sistventas.backend.dto.SubcategoriaDto;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/{categoriaId}/subcategorias/{subcategoriaId}/imagen", consumes = "multipart/form-data")
    public ResponseEntity<SubcategoriaDto> actualizarImagenSubcategoria(
            @PathVariable Long categoriaId,
            @PathVariable Long subcategoriaId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(categoriaService.actualizarImagenSubcategoria(categoriaId, subcategoriaId, file, principal));
    }

    @DeleteMapping("/{categoriaId}/subcategorias/{subcategoriaId}/imagen")
    public ResponseEntity<SubcategoriaDto> eliminarImagenSubcategoria(
            @PathVariable Long categoriaId,
            @PathVariable Long subcategoriaId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(categoriaService.eliminarImagenSubcategoria(categoriaId, subcategoriaId, principal));
    }

    @GetMapping("/{categoriaId}/atributos")
    public ResponseEntity<List<AtributoFiltroDto>> listarAtributosFiltro(
            @PathVariable Long categoriaId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(categoriaService.listarAtributosFiltro(categoriaId, principal));
    }

    @PostMapping("/{categoriaId}/atributos")
    public ResponseEntity<AtributoFiltroDto> crearAtributoFiltro(
            @PathVariable Long categoriaId,
            @Valid @RequestBody CrearAtributoFiltroRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AtributoFiltroDto dto = categoriaService.crearAtributoFiltro(categoriaId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/{categoriaId}/atributos/{atributoId}")
    public ResponseEntity<Void> eliminarAtributoFiltro(
            @PathVariable Long categoriaId,
            @PathVariable Long atributoId,
            @AuthenticationPrincipal UserPrincipal principal) {
        categoriaService.eliminarAtributoFiltro(categoriaId, atributoId, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{categoriaId}/atributos/{atributoId}/valores")
    public ResponseEntity<AtributoFiltroDto> crearValorAtributoFiltro(
            @PathVariable Long categoriaId,
            @PathVariable Long atributoId,
            @Valid @RequestBody CrearAtributoFiltroValorRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AtributoFiltroDto dto = categoriaService.crearValorAtributoFiltro(categoriaId, atributoId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/{categoriaId}/atributos/{atributoId}/valores/{valorId}")
    public ResponseEntity<Void> eliminarValorAtributoFiltro(
            @PathVariable Long categoriaId,
            @PathVariable Long atributoId,
            @PathVariable Long valorId,
            @AuthenticationPrincipal UserPrincipal principal) {
        categoriaService.eliminarValorAtributoFiltro(categoriaId, atributoId, valorId, principal);
        return ResponseEntity.noContent().build();
    }
}
