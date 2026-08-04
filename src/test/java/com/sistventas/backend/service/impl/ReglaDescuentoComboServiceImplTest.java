package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarReglaDescuentoComboRequest;
import com.sistventas.backend.dto.CategoriaSubcategoriaDto;
import com.sistventas.backend.dto.CrearReglaDescuentoComboRequest;
import com.sistventas.backend.dto.ReglaDescuentoComboDto;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests del CRUD admin de reglas de descuento combo (ReglaDescuentoComboServiceImpl):
// alta/edición/listado/borrado siempre scopeados a la empresa del principal, y la
// validación de que las 4 referencias (categoría/subcategoría de cada lado) existan
// y pertenezcan a esta empresa/categoría antes de guardar. El CÁLCULO del descuento en
// sí (pares, mejor regla gana, redondeo) ya está cubierto en
// CalculadorDescuentoComboServiceTest — acá no se repite esa lógica.
@ExtendWith(MockitoExtension.class)
class ReglaDescuentoComboServiceImplTest {

    private static final Long EMPRESA_ID = 1L;
    private static final Long CAT_MATES = 10L;
    private static final Long CAT_BOMBILLAS = 20L;
    private static final Long SUB_ALGARROBO = 100L;
    private static final Long SUB_PALO_SANTO = 101L;

    @Mock
    private ReglaDescuentoComboRepository reglaDescuentoComboRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private SubcategoriaRepository subcategoriaRepository;

    @InjectMocks
    private ReglaDescuentoComboServiceImpl service;

    private final UserPrincipal admin = new UserPrincipal(1L, EMPRESA_ID, false, RolEmpresa.ADMIN);
    private final UserPrincipal member = new UserPrincipal(2L, EMPRESA_ID, false, RolEmpresa.MEMBER);
    private final UserPrincipal sinEmpresa = new UserPrincipal(3L, null, false, null);

    // ---------- listar ----------

    @Test
    void listarConAdminDevuelveSoloLasReglasDeSuEmpresaMapeadasADto() {
        ReglaDescuentoCombo regla = reglaSinSubcategorias(CAT_MATES, CAT_BOMBILLAS, "10");
        when(reglaDescuentoComboRepository.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of(regla));
        when(categoriaRepository.findById(CAT_MATES)).thenReturn(Optional.of(categoria(CAT_MATES, "Mates")));
        when(categoriaRepository.findById(CAT_BOMBILLAS)).thenReturn(Optional.of(categoria(CAT_BOMBILLAS, "Bombillas")));

        List<ReglaDescuentoComboDto> resultado = service.listar(admin);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).categoriaANombre()).isEqualTo("Mates");
        assertThat(resultado.get(0).categoriaBNombre()).isEqualTo("Bombillas");
    }

    @Test
    void listarConRolMemberLanzaAccesoRestringidoAdminException() {
        assertThatThrownBy(() -> service.listar(member))
                .isInstanceOf(AccesoRestringidoAdminException.class);

        verify(reglaDescuentoComboRepository, never()).findByEmpresaId(any());
    }

    @Test
    void listarSinEmpresaLanzaSinEmpresaException() {
        assertThatThrownBy(() -> service.listar(sinEmpresa))
                .isInstanceOf(SinEmpresaException.class);
    }

    // ---------- crear ----------

    @Test
    void crearConCategoriasValidasYActivoNullPersisteComoActiva() {
        CrearReglaDescuentoComboRequest request = new CrearReglaDescuentoComboRequest(
                CAT_MATES, null, CAT_BOMBILLAS, null, new BigDecimal("10"), null);
        stubCategoriasValidas();
        when(reglaDescuentoComboRepository.save(any(ReglaDescuentoCombo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReglaDescuentoComboDto resultado = service.crear(request, admin);

        assertThat(resultado.activo()).isTrue();
        assertThat(resultado.porcentaje()).isEqualByComparingTo("10");
    }

    @Test
    void crearConActivoFalseExplicitoRespetaFalse() {
        CrearReglaDescuentoComboRequest request = new CrearReglaDescuentoComboRequest(
                CAT_MATES, null, CAT_BOMBILLAS, null, new BigDecimal("10"), false);
        stubCategoriasValidas();
        when(reglaDescuentoComboRepository.save(any(ReglaDescuentoCombo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReglaDescuentoComboDto resultado = service.crear(request, admin);

        assertThat(resultado.activo()).isFalse();
    }

    @Test
    void crearConRolMemberLanzaAccesoRestringidoAdminException() {
        CrearReglaDescuentoComboRequest request = new CrearReglaDescuentoComboRequest(
                CAT_MATES, null, CAT_BOMBILLAS, null, new BigDecimal("10"), null);

        assertThatThrownBy(() -> service.crear(request, member))
                .isInstanceOf(AccesoRestringidoAdminException.class);

        verify(reglaDescuentoComboRepository, never()).save(any());
    }

    @Test
    void crearConCategoriaAInexistenteODeOtraEmpresaLanzaCategoriaNoEncontradaException() {
        CrearReglaDescuentoComboRequest request = new CrearReglaDescuentoComboRequest(
                CAT_MATES, null, CAT_BOMBILLAS, null, new BigDecimal("10"), null);
        when(categoriaRepository.findByIdAndEmpresaId(CAT_MATES, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(request, admin))
                .isInstanceOf(CategoriaNoEncontradaException.class);

        verify(reglaDescuentoComboRepository, never()).save(any());
    }

    @Test
    void crearConSubcategoriaQueNoPerteneceALaCategoriaElegidaLanzaSubcategoriaNoEncontradaException() {
        CrearReglaDescuentoComboRequest request = new CrearReglaDescuentoComboRequest(
                CAT_MATES, SUB_ALGARROBO, CAT_BOMBILLAS, null, new BigDecimal("10"), null);
        when(categoriaRepository.findByIdAndEmpresaId(CAT_MATES, EMPRESA_ID))
                .thenReturn(Optional.of(categoria(CAT_MATES, "Mates")));
        when(categoriaRepository.findByIdAndEmpresaId(CAT_BOMBILLAS, EMPRESA_ID))
                .thenReturn(Optional.of(categoria(CAT_BOMBILLAS, "Bombillas")));
        when(subcategoriaRepository.findByIdAndCategoriaId(SUB_ALGARROBO, CAT_MATES)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(request, admin))
                .isInstanceOf(SubcategoriaNoEncontradaException.class);

        verify(reglaDescuentoComboRepository, never()).save(any());
    }

    @Test
    void crearConAmbosLadosIdenticosCategoriaYSubcategoriaLanzaReglaDescuentoComboInvalidaException() {
        // Mismo id de categoría y misma subcategoría en los dos lados: un
        // producto se aparearía consigo mismo.
        CrearReglaDescuentoComboRequest request = new CrearReglaDescuentoComboRequest(
                CAT_MATES, SUB_ALGARROBO, CAT_MATES, SUB_ALGARROBO, new BigDecimal("10"), null);
        when(categoriaRepository.findByIdAndEmpresaId(CAT_MATES, EMPRESA_ID))
                .thenReturn(Optional.of(categoria(CAT_MATES, "Mates")));
        when(subcategoriaRepository.findByIdAndCategoriaId(SUB_ALGARROBO, CAT_MATES))
                .thenReturn(Optional.of(subcategoria(SUB_ALGARROBO, CAT_MATES, "Algarrobo")));

        assertThatThrownBy(() -> service.crear(request, admin))
                .isInstanceOf(ReglaDescuentoComboInvalidaException.class);

        verify(reglaDescuentoComboRepository, never()).save(any());
    }

    @Test
    void crearConMismaCategoriaYUnLadoSinSubcategoriaLanzaReglaDescuentoComboInvalidaException() {
        // Misma categoría en los dos lados, y el lado B no tiene subcategoría
        // (matchea cualquiera de esa categoría): el lado A (con subcategoría
        // puntual) queda incluido dentro del conjunto del lado B, mismo
        // problema de auto-pareo aunque no sean el mismo id exacto.
        CrearReglaDescuentoComboRequest request = new CrearReglaDescuentoComboRequest(
                CAT_MATES, SUB_ALGARROBO, CAT_MATES, null, new BigDecimal("10"), null);
        when(categoriaRepository.findByIdAndEmpresaId(CAT_MATES, EMPRESA_ID))
                .thenReturn(Optional.of(categoria(CAT_MATES, "Mates")));
        when(subcategoriaRepository.findByIdAndCategoriaId(SUB_ALGARROBO, CAT_MATES))
                .thenReturn(Optional.of(subcategoria(SUB_ALGARROBO, CAT_MATES, "Algarrobo")));

        assertThatThrownBy(() -> service.crear(request, admin))
                .isInstanceOf(ReglaDescuentoComboInvalidaException.class);
    }

    @Test
    void crearConMismaCategoriaPeroSubcategoriasDistintasEnAmbosLadosNoLanza() {
        // Misma categoría, pero cada lado exige una subcategoría puntual y
        // distinta: los conjuntos no se superponen, es una regla válida.
        CrearReglaDescuentoComboRequest request = new CrearReglaDescuentoComboRequest(
                CAT_MATES, SUB_ALGARROBO, CAT_MATES, SUB_PALO_SANTO, new BigDecimal("10"), null);
        when(categoriaRepository.findByIdAndEmpresaId(CAT_MATES, EMPRESA_ID))
                .thenReturn(Optional.of(categoria(CAT_MATES, "Mates")));
        when(subcategoriaRepository.findByIdAndCategoriaId(SUB_ALGARROBO, CAT_MATES))
                .thenReturn(Optional.of(subcategoria(SUB_ALGARROBO, CAT_MATES, "Algarrobo")));
        when(subcategoriaRepository.findByIdAndCategoriaId(SUB_PALO_SANTO, CAT_MATES))
                .thenReturn(Optional.of(subcategoria(SUB_PALO_SANTO, CAT_MATES, "Palo santo")));
        when(reglaDescuentoComboRepository.save(any(ReglaDescuentoCombo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReglaDescuentoComboDto resultado = service.crear(request, admin);

        assertThat(resultado.categoriaAId()).isEqualTo(CAT_MATES);
        assertThat(resultado.categoriaBId()).isEqualTo(CAT_MATES);
    }

    // ---------- actualizar ----------

    @Test
    void actualizarReglaExistenteDeLaEmpresaActualizaCampos() {
        ReglaDescuentoCombo existente = reglaSinSubcategorias(CAT_MATES, CAT_BOMBILLAS, "10");
        existente.setId(5L);
        when(reglaDescuentoComboRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        stubCategoriasValidas();
        when(reglaDescuentoComboRepository.save(any(ReglaDescuentoCombo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ActualizarReglaDescuentoComboRequest request = new ActualizarReglaDescuentoComboRequest(
                CAT_MATES, null, CAT_BOMBILLAS, null, new BigDecimal("25"), false);

        ReglaDescuentoComboDto resultado = service.actualizar(5L, request, admin);

        assertThat(resultado.porcentaje()).isEqualByComparingTo("25");
        assertThat(resultado.activo()).isFalse();
    }

    @Test
    void actualizarReglaDeOtraEmpresaLanzaReglaDescuentoComboNoEncontradaException() {
        when(reglaDescuentoComboRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.empty());

        ActualizarReglaDescuentoComboRequest request = new ActualizarReglaDescuentoComboRequest(
                CAT_MATES, null, CAT_BOMBILLAS, null, new BigDecimal("25"), true);

        assertThatThrownBy(() -> service.actualizar(5L, request, admin))
                .isInstanceOf(ReglaDescuentoComboNoEncontradaException.class);

        verify(reglaDescuentoComboRepository, never()).save(any());
    }

    @Test
    void actualizarConRolMemberLanzaAccesoRestringidoAdminException() {
        ActualizarReglaDescuentoComboRequest request = new ActualizarReglaDescuentoComboRequest(
                CAT_MATES, null, CAT_BOMBILLAS, null, new BigDecimal("25"), true);

        assertThatThrownBy(() -> service.actualizar(5L, request, member))
                .isInstanceOf(AccesoRestringidoAdminException.class);

        verify(reglaDescuentoComboRepository, never()).findByIdAndEmpresaId(any(), any());
    }

    // ---------- eliminar ----------

    @Test
    void eliminarReglaDeLaEmpresaLaBorra() {
        ReglaDescuentoCombo existente = reglaSinSubcategorias(CAT_MATES, CAT_BOMBILLAS, "10");
        existente.setId(5L);
        when(reglaDescuentoComboRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));

        service.eliminar(5L, admin);

        verify(reglaDescuentoComboRepository).delete(existente);
    }

    @Test
    void eliminarReglaDeOtraEmpresaLanzaReglaDescuentoComboNoEncontradaExceptionYNoBorraNada() {
        when(reglaDescuentoComboRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(5L, admin))
                .isInstanceOf(ReglaDescuentoComboNoEncontradaException.class);

        verify(reglaDescuentoComboRepository, never()).delete(any());
    }

    // ---------- listarCategoriasSubcategorias ----------

    @Test
    void listarCategoriasSubcategoriasDevuelveElCatalogoDeLaEmpresaConSusSubcategorias() {
        Categoria mates = categoria(CAT_MATES, "Mates");
        when(categoriaRepository.findByEmpresaIdOrderByNombreAsc(EMPRESA_ID)).thenReturn(List.of(mates));
        when(subcategoriaRepository.findByCategoriaIdOrderByNombreAsc(CAT_MATES))
                .thenReturn(List.of(subcategoria(SUB_ALGARROBO, CAT_MATES, "Algarrobo")));

        List<CategoriaSubcategoriaDto> resultado = service.listarCategoriasSubcategorias(admin);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).categoria()).isEqualTo("Mates");
        assertThat(resultado.get(0).subcategorias()).hasSize(1);
        assertThat(resultado.get(0).subcategorias().get(0).nombre()).isEqualTo("Algarrobo");
    }

    // ---------- helpers ----------

    private void stubCategoriasValidas() {
        when(categoriaRepository.findByIdAndEmpresaId(CAT_MATES, EMPRESA_ID))
                .thenReturn(Optional.of(categoria(CAT_MATES, "Mates")));
        when(categoriaRepository.findByIdAndEmpresaId(CAT_BOMBILLAS, EMPRESA_ID))
                .thenReturn(Optional.of(categoria(CAT_BOMBILLAS, "Bombillas")));
    }

    private Categoria categoria(Long id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setEmpresaId(EMPRESA_ID);
        categoria.setNombre(nombre);
        return categoria;
    }

    private Subcategoria subcategoria(Long id, Long categoriaId, String nombre) {
        Subcategoria subcategoria = new Subcategoria();
        subcategoria.setId(id);
        subcategoria.setCategoriaId(categoriaId);
        subcategoria.setNombre(nombre);
        return subcategoria;
    }

    private ReglaDescuentoCombo reglaSinSubcategorias(Long categoriaAId, Long categoriaBId, String porcentaje) {
        ReglaDescuentoCombo regla = new ReglaDescuentoCombo();
        regla.setEmpresaId(EMPRESA_ID);
        regla.setCategoriaAId(categoriaAId);
        regla.setCategoriaBId(categoriaBId);
        regla.setPorcentaje(new BigDecimal(porcentaje));
        regla.setActivo(true);
        return regla;
    }
}
