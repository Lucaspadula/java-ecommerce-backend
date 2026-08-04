package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.CategoriaDto;
import com.sistventas.backend.dto.CrearCategoriaRequest;
import com.sistventas.backend.dto.CrearSubcategoriaRequest;
import com.sistventas.backend.dto.SubcategoriaDto;
import com.sistventas.backend.entity.Categoria;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.Subcategoria;
import com.sistventas.backend.exception.CategoriaNoEncontradaException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.CategoriaRepository;
import com.sistventas.backend.repository.SubcategoriaRepository;
import com.sistventas.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// El service no valida nombre duplicado con excepción propia: crear() y
// crearSubcategoria() son find-or-create (ver CrearCategoriaRequest /
// CrearSubcategoriaRequest), evitan duplicados por mayúsculas/espacios
// devolviendo la fila existente en vez de fallar. Todo el scoping
// multiempresa pasa por empresaId sacado del UserPrincipal, nunca del
// cliente.
@ExtendWith(MockitoExtension.class)
class CategoriaServiceImplTest {

    private static final Long EMPRESA_ID = 1L;
    private static final Long CATEGORIA_ID = 10L;

    @Mock private CategoriaRepository categoriaRepository;
    @Mock private SubcategoriaRepository subcategoriaRepository;

    private CategoriaServiceImpl service;

    private final UserPrincipal principal = new UserPrincipal(1L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @BeforeEach
    void setUp() {
        service = new CategoriaServiceImpl(categoriaRepository, subcategoriaRepository);
    }

    // --- listar ---

    @Test
    void listarDevuelveSoloLasCategoriasDeLaEmpresaDelPrincipal() {
        Categoria categoria = categoriaConId(CATEGORIA_ID, "Accesorios");
        when(categoriaRepository.findByEmpresaIdOrderByNombreAsc(EMPRESA_ID)).thenReturn(List.of(categoria));

        List<CategoriaDto> resultado = service.listar(principal);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Accesorios");
    }

    @Test
    void listarSinEmpresaEnElPrincipalLanzaSinEmpresaException() {
        UserPrincipal superAdmin = new UserPrincipal(1L, null, true, null);

        assertThatThrownBy(() -> service.listar(superAdmin))
                .isInstanceOf(SinEmpresaException.class);
    }

    // --- crear (find-or-create) ---

    @Test
    void crearConNombreNuevoPersisteUnaCategoriaNueva() {
        when(categoriaRepository.findByEmpresaIdAndNombreIgnoreCase(EMPRESA_ID, "Mates"))
                .thenReturn(Optional.empty());
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoriaDto resultado = service.crear(new CrearCategoriaRequest("Mates"), principal);

        assertThat(resultado.nombre()).isEqualTo("Mates");
        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    void crearConNombreYaExistenteIgnorandoMayusculasDevuelveLaCategoriaExistenteSinDuplicar() {
        Categoria existente = categoriaConId(CATEGORIA_ID, "Mates");
        when(categoriaRepository.findByEmpresaIdAndNombreIgnoreCase(EMPRESA_ID, "mates"))
                .thenReturn(Optional.of(existente));

        CategoriaDto resultado = service.crear(new CrearCategoriaRequest("mates"), principal);

        assertThat(resultado.id()).isEqualTo(CATEGORIA_ID);
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void crearRecortaEspaciosDelNombreAntesDeBuscarloYPersistirlo() {
        when(categoriaRepository.findByEmpresaIdAndNombreIgnoreCase(EMPRESA_ID, "Mates"))
                .thenReturn(Optional.empty());
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoriaDto resultado = service.crear(new CrearCategoriaRequest("  Mates  "), principal);

        assertThat(resultado.nombre()).isEqualTo("Mates");
        verify(categoriaRepository).findByEmpresaIdAndNombreIgnoreCase(EMPRESA_ID, "Mates");
    }

    // --- listarSubcategorias ---

    @Test
    void listarSubcategoriasDeCategoriaValidaDelegaEnElRepository() {
        Categoria categoria = categoriaConId(CATEGORIA_ID, "Accesorios");
        when(categoriaRepository.findByIdAndEmpresaId(CATEGORIA_ID, EMPRESA_ID)).thenReturn(Optional.of(categoria));
        Subcategoria subcategoria = subcategoriaConId(100L, "Bombillas");
        when(subcategoriaRepository.findByCategoriaIdOrderByNombreAsc(CATEGORIA_ID))
                .thenReturn(List.of(subcategoria));

        List<SubcategoriaDto> resultado = service.listarSubcategorias(CATEGORIA_ID, principal);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Bombillas");
    }

    @Test
    void listarSubcategoriasDeCategoriaInexistenteOAjenaLanzaCategoriaNoEncontrada() {
        when(categoriaRepository.findByIdAndEmpresaId(CATEGORIA_ID, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarSubcategorias(CATEGORIA_ID, principal))
                .isInstanceOf(CategoriaNoEncontradaException.class);

        verify(subcategoriaRepository, never()).findByCategoriaIdOrderByNombreAsc(any());
    }

    // --- crearSubcategoria (find-or-create) ---

    @Test
    void crearSubcategoriaConNombreNuevoLaPersisteAsociadaALaCategoriaCorrecta() {
        Categoria categoria = categoriaConId(CATEGORIA_ID, "Accesorios");
        when(categoriaRepository.findByIdAndEmpresaId(CATEGORIA_ID, EMPRESA_ID)).thenReturn(Optional.of(categoria));
        when(subcategoriaRepository.findByCategoriaIdAndNombreIgnoreCase(CATEGORIA_ID, "Bombillas"))
                .thenReturn(Optional.empty());
        when(subcategoriaRepository.save(any(Subcategoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubcategoriaDto resultado = service.crearSubcategoria(
                CATEGORIA_ID, new CrearSubcategoriaRequest("Bombillas"), principal);

        assertThat(resultado.nombre()).isEqualTo("Bombillas");
        ArgumentCaptor<Subcategoria> captor = ArgumentCaptor.forClass(Subcategoria.class);
        verify(subcategoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getCategoriaId()).isEqualTo(CATEGORIA_ID);
    }

    @Test
    void crearSubcategoriaConNombreYaExistenteDevuelveLaExistenteSinDuplicar() {
        Categoria categoria = categoriaConId(CATEGORIA_ID, "Accesorios");
        when(categoriaRepository.findByIdAndEmpresaId(CATEGORIA_ID, EMPRESA_ID)).thenReturn(Optional.of(categoria));
        Subcategoria existente = subcategoriaConId(100L, "Bombillas");
        when(subcategoriaRepository.findByCategoriaIdAndNombreIgnoreCase(CATEGORIA_ID, "bombillas"))
                .thenReturn(Optional.of(existente));

        SubcategoriaDto resultado = service.crearSubcategoria(
                CATEGORIA_ID, new CrearSubcategoriaRequest("bombillas"), principal);

        assertThat(resultado.id()).isEqualTo(100L);
        verify(subcategoriaRepository, never()).save(any());
    }

    @Test
    void crearSubcategoriaDeCategoriaInexistenteOAjenaLanzaCategoriaNoEncontrada() {
        when(categoriaRepository.findByIdAndEmpresaId(CATEGORIA_ID, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearSubcategoria(
                CATEGORIA_ID, new CrearSubcategoriaRequest("Bombillas"), principal))
                .isInstanceOf(CategoriaNoEncontradaException.class);

        verify(subcategoriaRepository, never()).save(any());
    }

    private Categoria categoriaConId(Long id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setEmpresaId(EMPRESA_ID);
        categoria.setNombre(nombre);
        return categoria;
    }

    private Subcategoria subcategoriaConId(Long id, String nombre) {
        Subcategoria subcategoria = new Subcategoria();
        subcategoria.setId(id);
        subcategoria.setCategoriaId(CATEGORIA_ID);
        subcategoria.setNombre(nombre);
        return subcategoria;
    }
}
