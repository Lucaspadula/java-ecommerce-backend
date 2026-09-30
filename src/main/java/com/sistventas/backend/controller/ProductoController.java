package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ActualizarProductoRequest;
import com.sistventas.backend.dto.AjustePrecioCategoriaRequest;
import com.sistventas.backend.dto.AjustePrecioCategoriaResultadoDto;
import com.sistventas.backend.dto.AjustarFotoRequest;
import com.sistventas.backend.dto.AsignarColorFotoRequest;
import com.sistventas.backend.dto.CrearResenaRequest;
import com.sistventas.backend.dto.DescripcionIaResponse;
import com.sistventas.backend.dto.FotoUploadDto;
import com.sistventas.backend.dto.ProductoDto;
import com.sistventas.backend.dto.ProductoRequest;
import com.sistventas.backend.dto.ReordenarFotosRequest;
import com.sistventas.backend.dto.ResenaDto;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.CatalogoService;
import com.sistventas.backend.service.DescripcionIaService;
import com.sistventas.backend.service.ProductoService;
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
 * Catálogo de productos de la empresa del usuario logueado. Todo el scoping
 * multiempresa se resuelve vía UserPrincipal (@AuthenticationPrincipal) —
 * ningún método acá confía en un empresaId del body/query del cliente.
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final CatalogoService catalogoService;
    private final DescripcionIaService descripcionIaService;

    public ProductoController(
            ProductoService productoService,
            CatalogoService catalogoService,
            DescripcionIaService descripcionIaService) {
        this.productoService = productoService;
        this.catalogoService = catalogoService;
        this.descripcionIaService = descripcionIaService;
    }

    @GetMapping
    public ResponseEntity<List<ProductoDto>> listar(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(productoService.listar(principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoDto> obtener(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(productoService.obtener(id, principal));
    }

    @PostMapping
    public ResponseEntity<ProductoDto> crear(
            @Valid @RequestBody ProductoRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ProductoDto dto = productoService.crear(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarProductoRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(productoService.actualizar(id, request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        productoService.eliminar(id, principal);
        return ResponseEntity.noContent().build();
    }

    // Pool unificado de fotos (reemplaza el viejo esquema de slots fijos
    // 1-4): hasta 8 por producto, cada una con varianteId opcional (color).
    // varianteId ausente = foto "general".
    @PostMapping(value = "/{id}/fotos", consumes = "multipart/form-data")
    public ResponseEntity<ProductoDto> agregarFoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "varianteId", required = false) Long varianteId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(productoService.agregarFoto(id, file, varianteId, principal));
    }

    @DeleteMapping("/{id}/fotos/{fotoId}")
    public ResponseEntity<ProductoDto> eliminarFoto(
            @PathVariable Long id,
            @PathVariable Long fotoId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(productoService.eliminarFoto(id, fotoId, principal));
    }

    // Cambia el color asociado a una foto sin re-subir el archivo (ver spec
    // "Cambio de color sin re-subida"). varianteId null en el body = quita el
    // color (vuelve a "general").
    @PatchMapping("/{id}/fotos/{fotoId}")
    public ResponseEntity<ProductoDto> asignarColorFoto(
            @PathVariable Long id,
            @PathVariable Long fotoId,
            @RequestBody AsignarColorFotoRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(productoService.asignarColorFoto(id, fotoId, request.varianteId(), principal));
    }

    // Toggle manual del ajuste de una foto (ver spec "Ajuste manual de
    // foto"): el dueño elige, foto por foto, si se ve completa (contain) o
    // agrandada llenando el marco (cover) — antes lo decidía el CSS solo.
    @PatchMapping("/{id}/fotos/{fotoId}/ajuste")
    public ResponseEntity<ProductoDto> ajustarFoto(
            @PathVariable Long id,
            @PathVariable Long fotoId,
            @RequestBody AjustarFotoRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(productoService.ajustarFoto(id, fotoId, request.agrandada(), principal));
    }

    // Endpoint dedicado (no embebido en ActualizarProductoRequest, ver design
    // Decision #1): body { "fotoIds": [12, 9, 15] } con TODOS los ids del
    // producto en el orden final deseado.
    @PutMapping("/{id}/fotos/orden")
    public ResponseEntity<ProductoDto> reordenarFotos(
            @PathVariable Long id,
            @Valid @RequestBody ReordenarFotosRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(productoService.reordenarFotos(id, request.fotoIds(), principal));
    }

    // Genérico, no atado a un producto/variante puntual — ver
    // ProductoService.subirFotoVariante. Path fijo "/variantes/foto" (no
    // "/{id}/...") para no chocar con el mapping de {id} de arriba.
    @PostMapping(value = "/variantes/foto", consumes = "multipart/form-data")
    public ResponseEntity<FotoUploadDto> subirFotoVariante(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(productoService.subirFotoVariante(file, principal));
    }

    // Sin {id}: se genera con la foto recién elegida, antes incluso de que
    // el producto exista (ver productos.ts § generarDescripcionIa en el
    // frontend). "nombre" es opcional, solo suma contexto al prompt.
    @PostMapping(value = "/descripcion-ia", consumes = "multipart/form-data")
    public ResponseEntity<DescripcionIaResponse> generarDescripcionIa(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "nombre", required = false) String nombre,
            @AuthenticationPrincipal UserPrincipal principal) {
        String descripcion = descripcionIaService.generar(file, nombre, principal);
        return ResponseEntity.ok(new DescripcionIaResponse(descripcion));
    }

    @PatchMapping("/ajuste-por-categoria")
    public ResponseEntity<AjustePrecioCategoriaResultadoDto> ajustarPrecioPorCategoria(
            @Valid @RequestBody AjustePrecioCategoriaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(productoService.ajustarPrecioPorCategoria(request, principal));
    }

    @GetMapping("/{id}/resenas")
    public ResponseEntity<List<ResenaDto>> listarResenas(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(productoService.listarResenas(id, principal));
    }

    @PostMapping("/{id}/resenas")
    public ResponseEntity<ResenaDto> crearResena(
            @PathVariable Long id,
            @Valid @RequestBody CrearResenaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ResenaDto dto = productoService.crearResena(id, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/{id}/resenas/{resenaId}")
    public ResponseEntity<Void> eliminarResena(
            @PathVariable Long id,
            @PathVariable Long resenaId,
            @AuthenticationPrincipal UserPrincipal principal) {
        productoService.eliminarResena(id, resenaId, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/resenas/{resenaId}/foto", consumes = "multipart/form-data")
    public ResponseEntity<ResenaDto> actualizarFotoResena(
            @PathVariable Long id,
            @PathVariable Long resenaId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(productoService.actualizarFotoResena(id, resenaId, file, principal));
    }

    @GetMapping("/catalogo/pdf")
    public ResponseEntity<byte[]> catalogoPdf(@AuthenticationPrincipal UserPrincipal principal) {
        byte[] pdf = catalogoService.generarPdf(principal);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"catalogo.pdf\"")
                .body(pdf);
    }

    @GetMapping("/catalogo/excel")
    public ResponseEntity<byte[]> catalogoExcel(@AuthenticationPrincipal UserPrincipal principal) {
        byte[] excel = catalogoService.generarExcel(principal);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"catalogo.xlsx\"")
                .body(excel);
    }
}
