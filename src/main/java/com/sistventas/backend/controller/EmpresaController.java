package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ActualizarColorCategoriaRequest;
import com.sistventas.backend.dto.ActualizarProductoBannerImagenRequest;
import com.sistventas.backend.dto.ActualizarReglaDescuentoComboRequest;
import com.sistventas.backend.dto.ActualizarTiendaRequest;
import com.sistventas.backend.dto.BannerImagenTiendaDto;
import com.sistventas.backend.dto.CategoriaSubcategoriaDto;
import com.sistventas.backend.dto.CategoriaTiendaDto;
import com.sistventas.backend.dto.CrearReglaDescuentoComboRequest;
import com.sistventas.backend.dto.CrearTestimonioRequest;
import com.sistventas.backend.dto.CrearTipRequest;
import com.sistventas.backend.dto.PerfilDto;
import com.sistventas.backend.dto.ReglaDescuentoComboDto;
import com.sistventas.backend.dto.TestimonioDto;
import com.sistventas.backend.dto.TipDto;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.PerfilService;
import com.sistventas.backend.service.ReglaDescuentoComboService;
import com.sistventas.backend.service.TiendaCategoriaService;
import com.sistventas.backend.service.TiendaTestimonioService;
import com.sistventas.backend.service.TiendaTipService;
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
 * Datos de la empresa del usuario logueado. Separado de PerfilController
 * porque el path (/api/empresa) no comparte prefijo con /api/perfil, aunque
 * la lógica vive en PerfilService (el logo es conceptualmente parte del
 * "área de perfil", igual que empresaNombre en PerfilDto).
 */
@RestController
@RequestMapping("/api/empresa")
public class EmpresaController {

    private final PerfilService perfilService;
    private final TiendaCategoriaService tiendaCategoriaService;
    private final TiendaTestimonioService tiendaTestimonioService;
    private final TiendaTipService tiendaTipService;
    private final ReglaDescuentoComboService reglaDescuentoComboService;

    public EmpresaController(PerfilService perfilService,
                              TiendaCategoriaService tiendaCategoriaService,
                              TiendaTestimonioService tiendaTestimonioService,
                              TiendaTipService tiendaTipService,
                              ReglaDescuentoComboService reglaDescuentoComboService) {
        this.perfilService = perfilService;
        this.tiendaCategoriaService = tiendaCategoriaService;
        this.tiendaTestimonioService = tiendaTestimonioService;
        this.tiendaTipService = tiendaTipService;
        this.reglaDescuentoComboService = reglaDescuentoComboService;
    }

    @PostMapping(value = "/logo", consumes = "multipart/form-data")
    public ResponseEntity<PerfilDto> actualizarLogo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(perfilService.actualizarLogoEmpresa(file, principal));
    }

    @PutMapping("/tienda")
    public ResponseEntity<PerfilDto> actualizarTienda(
            @Valid @RequestBody ActualizarTiendaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(perfilService.actualizarTienda(request, principal));
    }

    @GetMapping("/tienda/banner-imagenes")
    public ResponseEntity<List<BannerImagenTiendaDto>> listarBannerImagenes(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(perfilService.listarBannerImagenes(principal));
    }

    // tipo default "HERO": un frontend viejo que no mande el campo (o el
    // flujo actual de subida del banner hero) sigue funcionando sin cambios.
    @PostMapping(value = "/tienda/banner-imagenes", consumes = "multipart/form-data")
    public ResponseEntity<BannerImagenTiendaDto> agregarBannerImagen(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "tipo", required = false, defaultValue = "HERO") String tipo,
            @RequestParam(value = "productoId", required = false) Long productoId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(perfilService.agregarBannerImagen(file, tipo, productoId, principal));
    }

    @DeleteMapping("/tienda/banner-imagenes/{id}")
    public ResponseEntity<Void> eliminarBannerImagen(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        perfilService.eliminarBannerImagen(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/tienda/banner-imagenes/{id}/producto")
    public ResponseEntity<BannerImagenTiendaDto> actualizarProductoBannerImagen(
            @PathVariable Long id,
            @RequestBody ActualizarProductoBannerImagenRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(perfilService.actualizarProductoBannerImagen(id, request.productoId(), principal));
    }

    @GetMapping("/tienda/categorias")
    public ResponseEntity<List<CategoriaTiendaDto>> listarCategorias(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tiendaCategoriaService.listar(principal));
    }

    @PutMapping("/tienda/categorias/{categoriaId}/color")
    public ResponseEntity<CategoriaTiendaDto> actualizarColorCategoria(
            @PathVariable Long categoriaId,
            @Valid @RequestBody ActualizarColorCategoriaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tiendaCategoriaService.actualizarColor(categoriaId, request, principal));
    }

    @PostMapping(value = "/tienda/categorias/{categoriaId}/imagen", consumes = "multipart/form-data")
    public ResponseEntity<CategoriaTiendaDto> actualizarImagenCategoria(
            @PathVariable Long categoriaId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tiendaCategoriaService.actualizarImagen(categoriaId, file, principal));
    }

    @DeleteMapping("/tienda/categorias/{categoriaId}/imagen")
    public ResponseEntity<CategoriaTiendaDto> eliminarImagenCategoria(
            @PathVariable Long categoriaId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tiendaCategoriaService.eliminarImagen(categoriaId, principal));
    }

    @GetMapping("/tienda/testimonios")
    public ResponseEntity<List<TestimonioDto>> listarTestimonios(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tiendaTestimonioService.listar(principal));
    }

    @PostMapping("/tienda/testimonios")
    public ResponseEntity<TestimonioDto> crearTestimonio(
            @Valid @RequestBody CrearTestimonioRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TestimonioDto dto = tiendaTestimonioService.crear(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/tienda/testimonios/{id}")
    public ResponseEntity<Void> eliminarTestimonio(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        tiendaTestimonioService.eliminar(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/tienda/testimonios/{id}/foto", consumes = "multipart/form-data")
    public ResponseEntity<TestimonioDto> actualizarFotoTestimonio(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tiendaTestimonioService.actualizarFoto(id, file, principal));
    }

    @GetMapping("/tienda/tips")
    public ResponseEntity<List<TipDto>> listarTips(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tiendaTipService.listar(principal));
    }

    @PostMapping("/tienda/tips")
    public ResponseEntity<TipDto> crearTip(
            @Valid @RequestBody CrearTipRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TipDto dto = tiendaTipService.crear(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/tienda/tips/{id}")
    public ResponseEntity<Void> eliminarTip(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        tiendaTipService.eliminar(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/tienda/tips/{id}/foto", consumes = "multipart/form-data")
    public ResponseEntity<TipDto> actualizarFotoTip(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tiendaTipService.actualizarFoto(id, file, principal));
    }

    // Categorías/subcategorías reales (de productos activos), para poblar
    // los selectors en cascada del form de reglas de descuento combo.
    @GetMapping("/tienda/categorias-subcategorias")
    public ResponseEntity<List<CategoriaSubcategoriaDto>> listarCategoriasSubcategorias(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(reglaDescuentoComboService.listarCategoriasSubcategorias(principal));
    }

    @GetMapping("/tienda/descuentos-combo")
    public ResponseEntity<List<ReglaDescuentoComboDto>> listarReglasDescuentoCombo(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(reglaDescuentoComboService.listar(principal));
    }

    @PostMapping("/tienda/descuentos-combo")
    public ResponseEntity<ReglaDescuentoComboDto> crearReglaDescuentoCombo(
            @Valid @RequestBody CrearReglaDescuentoComboRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReglaDescuentoComboDto dto = reglaDescuentoComboService.crear(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/tienda/descuentos-combo/{id}")
    public ResponseEntity<ReglaDescuentoComboDto> actualizarReglaDescuentoCombo(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarReglaDescuentoComboRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(reglaDescuentoComboService.actualizar(id, request, principal));
    }

    @DeleteMapping("/tienda/descuentos-combo/{id}")
    public ResponseEntity<Void> eliminarReglaDescuentoCombo(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        reglaDescuentoComboService.eliminar(id, principal);
        return ResponseEntity.noContent().build();
    }
}
