package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarCatalogoSeccionRequest;
import com.sistventas.backend.dto.CatalogoSeccionDto;
import com.sistventas.backend.dto.CrearCatalogoSeccionRequest;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.TiendaCatalogoSeccion;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.exception.AccionNoPermitidaException;
import com.sistventas.backend.exception.CatalogoSeccionNoEncontradaException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.TiendaCatalogoSeccionRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.TiendaCatalogoSeccionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Puntos/párrafos de las páginas "Importante" (políticas) y "Cómo comprar"
 * del catálogo generado en PDF (ver CatalogoServiceImpl.generarPdf). Mismo
 * criterio que TiendaTipServiceImpl: lista libre por empresa, el admin
 * agrega y saca puntos uno por uno.
 */
@Service
public class TiendaCatalogoSeccionServiceImpl implements TiendaCatalogoSeccionService {

    // Únicos dos valores válidos de `tipo` — corresponden 1 a 1 con las
    // páginas fijas del catálogo (ver CatalogoServiceImpl).
    private static final Set<String> TIPOS_VALIDOS = Set.of("IMPORTANTE", "COMO_COMPRAR");

    private final TiendaCatalogoSeccionRepository tiendaCatalogoSeccionRepository;

    public TiendaCatalogoSeccionServiceImpl(TiendaCatalogoSeccionRepository tiendaCatalogoSeccionRepository) {
        this.tiendaCatalogoSeccionRepository = tiendaCatalogoSeccionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoSeccionDto> listar(UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        return tiendaCatalogoSeccionRepository.findByEmpresaIdOrderByTipoAscOrdenAscIdAsc(empresaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public CatalogoSeccionDto crear(CrearCatalogoSeccionRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        String tipo = tipoValidoOThrow(request.tipo());

        TiendaCatalogoSeccion seccion = new TiendaCatalogoSeccion();
        seccion.setEmpresaId(empresaId);
        seccion.setTipo(tipo);
        seccion.setTexto(request.texto().trim());
        seccion.setAlineacion(alineacionValidaOThrow(request.alineacion()));
        seccion.setNegrita(Boolean.TRUE.equals(request.negrita()));
        seccion.setCursiva(Boolean.TRUE.equals(request.cursiva()));
        seccion.setSubrayado(Boolean.TRUE.equals(request.subrayado()));
        // Se agrega siempre al final de SU lista (tipo), mismo criterio que
        // TiendaTipServiceImpl.crear.
        seccion.setOrden((int) tiendaCatalogoSeccionRepository.countByEmpresaIdAndTipo(empresaId, tipo));

        return toDto(tiendaCatalogoSeccionRepository.save(seccion));
    }

    @Override
    @Transactional
    public CatalogoSeccionDto actualizar(Long id, ActualizarCatalogoSeccionRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        TiendaCatalogoSeccion seccion = tiendaCatalogoSeccionRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(CatalogoSeccionNoEncontradaException::new);

        seccion.setTexto(request.texto().trim());
        seccion.setAlineacion(alineacionValidaOThrow(request.alineacion()));
        seccion.setNegrita(Boolean.TRUE.equals(request.negrita()));
        seccion.setCursiva(Boolean.TRUE.equals(request.cursiva()));
        seccion.setSubrayado(Boolean.TRUE.equals(request.subrayado()));

        return toDto(seccion);
    }

    @Override
    @Transactional
    public void eliminar(Long id, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        TiendaCatalogoSeccion seccion = tiendaCatalogoSeccionRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(CatalogoSeccionNoEncontradaException::new);
        tiendaCatalogoSeccionRepository.delete(seccion);
    }

    // null/vacío (no elegido explícitamente) cae al default de siempre —
    // mismo criterio permisivo que el resto de los campos de estilo
    // opcionales de esta feature (V57, catalogo_texto_*).
    private static final String ALINEACION_DEFAULT = "IZQUIERDA";
    private static final Set<String> ALINEACIONES_VALIDAS = Set.of("IZQUIERDA", "CENTRO", "DERECHA");

    private String alineacionValidaOThrow(String alineacion) {
        if (alineacion == null || alineacion.isBlank()) {
            return ALINEACION_DEFAULT;
        }
        String normalizado = alineacion.trim().toUpperCase();
        if (!ALINEACIONES_VALIDAS.contains(normalizado)) {
            throw new AccionNoPermitidaException("La alineación debe ser IZQUIERDA, CENTRO o DERECHA");
        }
        return normalizado;
    }

    private String tipoValidoOThrow(String tipo) {
        String normalizado = tipo != null ? tipo.trim().toUpperCase() : "";
        if (!TIPOS_VALIDOS.contains(normalizado)) {
            throw new AccionNoPermitidaException("El tipo debe ser IMPORTANTE o COMO_COMPRAR");
        }
        return normalizado;
    }

    // Mismo criterio que TiendaTipServiceImpl.adminEmpresaIdOrThrow: la
    // personalización del catálogo solo la puede hacer el ADMIN.
    private Long adminEmpresaIdOrThrow(UserPrincipal principal) {
        if (principal == null || principal.empresaId() == null) {
            throw new SinEmpresaException();
        }
        if (principal.rolEmpresa() != RolEmpresa.ADMIN) {
            throw new AccesoRestringidoAdminException();
        }
        return principal.empresaId();
    }

    private CatalogoSeccionDto toDto(TiendaCatalogoSeccion seccion) {
        return new CatalogoSeccionDto(
                seccion.getId(),
                seccion.getTipo(),
                seccion.getTexto(),
                seccion.getOrden(),
                seccion.getAlineacion(),
                seccion.isNegrita(),
                seccion.isCursiva(),
                seccion.isSubrayado()
        );
    }
}
