package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarReglaDescuentoComboRequest;
import com.sistventas.backend.dto.CategoriaSubcategoriaDto;
import com.sistventas.backend.dto.CrearReglaDescuentoComboRequest;
import com.sistventas.backend.dto.ReglaDescuentoComboDto;
import com.sistventas.backend.dto.SubcategoriaDto;
import com.sistventas.backend.entity.Categoria;
import com.sistventas.backend.entity.ReglaDescuentoCombo;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.Subcategoria;
import com.sistventas.backend.exception.AccesoRestringidoAdminException;
import com.sistventas.backend.exception.CategoriaNoEncontradaException;
import com.sistventas.backend.exception.ReglaDescuentoComboInvalidaException;
import com.sistventas.backend.exception.ReglaDescuentoComboNoEncontradaException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.exception.SubcategoriaNoEncontradaException;
import com.sistventas.backend.repository.CategoriaRepository;
import com.sistventas.backend.repository.ReglaDescuentoComboRepository;
import com.sistventas.backend.repository.SubcategoriaRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.ReglaDescuentoComboService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD admin de reglas de descuento combo (ver ReglaDescuentoCombo y
 * CalculadorDescuentoComboService para el motor de cálculo que las consume
 * desde el checkout público). Mismo patrón que TiendaTestimonioServiceImpl /
 * TiendaTipServiceImpl: adminEmpresaIdOrThrow propio, sin mapper compartido.
 */
@Service
public class ReglaDescuentoComboServiceImpl implements ReglaDescuentoComboService {

    private final ReglaDescuentoComboRepository reglaDescuentoComboRepository;
    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaRepository subcategoriaRepository;

    public ReglaDescuentoComboServiceImpl(ReglaDescuentoComboRepository reglaDescuentoComboRepository,
                                           CategoriaRepository categoriaRepository,
                                           SubcategoriaRepository subcategoriaRepository) {
        this.reglaDescuentoComboRepository = reglaDescuentoComboRepository;
        this.categoriaRepository = categoriaRepository;
        this.subcategoriaRepository = subcategoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReglaDescuentoComboDto> listar(UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        return reglaDescuentoComboRepository.findByEmpresaId(empresaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ReglaDescuentoComboDto crear(CrearReglaDescuentoComboRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        validarLados(empresaId, request.categoriaAId(), request.subcategoriaAId(),
                request.categoriaBId(), request.subcategoriaBId());

        ReglaDescuentoCombo regla = new ReglaDescuentoCombo();
        regla.setEmpresaId(empresaId);
        regla.setCategoriaAId(request.categoriaAId());
        regla.setSubcategoriaAId(request.subcategoriaAId());
        regla.setCategoriaBId(request.categoriaBId());
        regla.setSubcategoriaBId(request.subcategoriaBId());
        regla.setPorcentaje(request.porcentaje());
        // null (campo no mandado) = activa por default, mismo criterio que el
        // resto de los "crear" del panel (ej. TiendaTestimonio no nace
        // inactivo).
        regla.setActivo(request.activo() == null || request.activo());

        return toDto(reglaDescuentoComboRepository.save(regla));
    }

    @Override
    @Transactional
    public ReglaDescuentoComboDto actualizar(Long id, ActualizarReglaDescuentoComboRequest request, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        ReglaDescuentoCombo regla = reglaDescuentoComboRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(ReglaDescuentoComboNoEncontradaException::new);
        validarLados(empresaId, request.categoriaAId(), request.subcategoriaAId(),
                request.categoriaBId(), request.subcategoriaBId());

        regla.setCategoriaAId(request.categoriaAId());
        regla.setSubcategoriaAId(request.subcategoriaAId());
        regla.setCategoriaBId(request.categoriaBId());
        regla.setSubcategoriaBId(request.subcategoriaBId());
        regla.setPorcentaje(request.porcentaje());
        regla.setActivo(request.activo());

        return toDto(reglaDescuentoComboRepository.save(regla));
    }

    @Override
    @Transactional
    public void eliminar(Long id, UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);
        ReglaDescuentoCombo regla = reglaDescuentoComboRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(ReglaDescuentoComboNoEncontradaException::new);
        reglaDescuentoComboRepository.delete(regla);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaSubcategoriaDto> listarCategoriasSubcategorias(UserPrincipal principal) {
        Long empresaId = adminEmpresaIdOrThrow(principal);

        // Lee directo del catálogo maestro (ver entities Categoria/
        // Subcategoria): ya no hace falta un distinct en memoria sobre
        // Producto, la tabla ES la fuente de verdad.
        return categoriaRepository.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .map(categoria -> new CategoriaSubcategoriaDto(
                        categoria.getId(),
                        categoria.getNombre(),
                        subcategoriaRepository.findByCategoriaIdOrderByNombreAsc(categoria.getId()).stream()
                                .map(sub -> new SubcategoriaDto(sub.getId(), sub.getNombre()))
                                .toList()))
                .toList();
    }

    // Valida que las 4 referencias existan y sean de ESTA empresa (categoría)
    // / de LA categoría elegida en ese mismo lado (subcategoría) antes de
    // guardarlas — nunca se confía en un id que venga del cliente sin
    // validar. Se hace acá (no en el DTO) porque necesita acceso a los
    // repositories.
    private void validarLados(Long empresaId, Long categoriaAId, Long subcategoriaAId,
                               Long categoriaBId, Long subcategoriaBId) {
        buscarCategoriaPorEmpresa(categoriaAId, empresaId);
        buscarCategoriaPorEmpresa(categoriaBId, empresaId);
        if (subcategoriaAId != null) {
            buscarSubcategoriaDeCategoria(subcategoriaAId, categoriaAId);
        }
        if (subcategoriaBId != null) {
            buscarSubcategoriaDeCategoria(subcategoriaBId, categoriaBId);
        }
        validarLadosDistintos(categoriaAId, subcategoriaAId, categoriaBId, subcategoriaBId);
    }

    // Evita una regla sin sentido donde los dos lados matchean exactamente lo
    // mismo (un producto no puede "parearse" consigo mismo) — el motor de
    // cálculo (CalculadorDescuentoComboService) no valida esto, asume que
    // ambos lados llegan ya distintos. No alcanza con rechazar el match
    // EXACTO: si la categoría es la misma y un lado no tiene subcategoría
    // (matchea cualquiera de esa categoría), un producto de la subcategoría
    // del otro lado cae en los dos conjuntos a la vez — mismo problema de
    // auto-pareo. Por eso el bloqueo es "misma categoría + (algún lado sin
    // subcategoría, o subcategorías iguales)", no solo la igualdad estricta.
    private void validarLadosDistintos(Long categoriaAId, Long subcategoriaAId, Long categoriaBId, Long subcategoriaBId) {
        boolean mismaCategoria = categoriaAId.equals(categoriaBId);
        boolean puedeSuperponerse = subcategoriaAId == null || subcategoriaBId == null
                || subcategoriaAId.equals(subcategoriaBId);
        if (mismaCategoria && puedeSuperponerse) {
            throw new ReglaDescuentoComboInvalidaException();
        }
    }

    private Categoria buscarCategoriaPorEmpresa(Long categoriaId, Long empresaId) {
        return categoriaRepository.findByIdAndEmpresaId(categoriaId, empresaId)
                .orElseThrow(CategoriaNoEncontradaException::new);
    }

    private Subcategoria buscarSubcategoriaDeCategoria(Long subcategoriaId, Long categoriaId) {
        return subcategoriaRepository.findByIdAndCategoriaId(subcategoriaId, categoriaId)
                .orElseThrow(SubcategoriaNoEncontradaException::new);
    }

    private Long adminEmpresaIdOrThrow(UserPrincipal principal) {
        if (principal == null || principal.empresaId() == null) {
            throw new SinEmpresaException();
        }
        if (principal.rolEmpresa() != RolEmpresa.ADMIN) {
            throw new AccesoRestringidoAdminException();
        }
        return principal.empresaId();
    }

    // Nombres resueltos con un findById suelto por cada lado presente (a lo
    // sumo 4 consultas chicas): volumen bajo (pocas reglas por empresa,
    // listado admin sin uso de alta frecuencia), no amerita una carga batch.
    private ReglaDescuentoComboDto toDto(ReglaDescuentoCombo regla) {
        return new ReglaDescuentoComboDto(
                regla.getId(),
                regla.getCategoriaAId(),
                nombreCategoria(regla.getCategoriaAId()),
                regla.getSubcategoriaAId(),
                nombreSubcategoria(regla.getSubcategoriaAId()),
                regla.getCategoriaBId(),
                nombreCategoria(regla.getCategoriaBId()),
                regla.getSubcategoriaBId(),
                nombreSubcategoria(regla.getSubcategoriaBId()),
                regla.getPorcentaje(),
                regla.isActivo()
        );
    }

    private String nombreCategoria(Long categoriaId) {
        return categoriaRepository.findById(categoriaId).map(Categoria::getNombre).orElse(null);
    }

    private String nombreSubcategoria(Long subcategoriaId) {
        if (subcategoriaId == null) {
            return null;
        }
        return subcategoriaRepository.findById(subcategoriaId).map(Subcategoria::getNombre).orElse(null);
    }
}
