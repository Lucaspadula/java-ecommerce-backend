package com.sistventas.backend.service;

import com.sistventas.backend.dto.ActualizarReglaDescuentoComboRequest;
import com.sistventas.backend.dto.CategoriaSubcategoriaDto;
import com.sistventas.backend.dto.CrearReglaDescuentoComboRequest;
import com.sistventas.backend.dto.ReglaDescuentoComboDto;
import com.sistventas.backend.security.UserPrincipal;

import java.util.List;

// Solo ADMIN (ver adminEmpresaIdOrThrow en la impl), mismo criterio que
// TiendaTestimonioService/TiendaTipService: configurar reglas de descuento es
// una acción de administración, no la puede hacer un MEMBER.
public interface ReglaDescuentoComboService {
    List<ReglaDescuentoComboDto> listar(UserPrincipal principal);

    ReglaDescuentoComboDto crear(CrearReglaDescuentoComboRequest request, UserPrincipal principal);

    ReglaDescuentoComboDto actualizar(Long id, ActualizarReglaDescuentoComboRequest request, UserPrincipal principal);

    void eliminar(Long id, UserPrincipal principal);

    // Categorías/subcategorías REALES (de productos activos), agrupadas para
    // poblar los selectors en cascada del form de reglas — no inventa un
    // catálogo separado del de Productos.
    List<CategoriaSubcategoriaDto> listarCategoriasSubcategorias(UserPrincipal principal);
}
