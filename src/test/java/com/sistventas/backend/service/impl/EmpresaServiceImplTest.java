package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ActualizarLicenciaRequest;
import com.sistventas.backend.dto.EmpresaAdminDto;
import com.sistventas.backend.entity.Empresa;
import com.sistventas.backend.entity.LicenciaEstado;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.entity.Usuario;
import com.sistventas.backend.exception.EmpresaNoEncontradaException;
import com.sistventas.backend.repository.EmpresaRepository;
import com.sistventas.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// EmpresaServiceImpl es la parte "admin" (Super Admin) de la gestión de
// empresas: listar con/sin filtro de licenciaEstado y actualizar la
// licencia de una empresa puntual. No tiene nada que ver con el logo ni con
// la tienda pública -- eso vive en PerfilServiceImpl, aunque ambos
// endpoints cuelguen de controllers con "Empresa" en el nombre.
@ExtendWith(MockitoExtension.class)
class EmpresaServiceImplTest {

    private static final Long EMPRESA_ID = 5L;

    @Mock private EmpresaRepository empresaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private EmpresaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmpresaServiceImpl(empresaRepository, usuarioRepository);
    }

    // --- listar ---

    @Test
    void listarSinEstadoDevuelveTodasLasEmpresas() {
        Empresa empresa = empresaConId(EMPRESA_ID, "Mi Tienda", LicenciaEstado.ACTIVA);
        when(empresaRepository.findAll()).thenReturn(List.of(empresa));
        when(usuarioRepository.findByEmpresaIdAndRolEmpresa(EMPRESA_ID, RolEmpresa.ADMIN)).thenReturn(List.of());

        List<EmpresaAdminDto> resultado = service.listar(null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Mi Tienda");
    }

    @Test
    void listarConEstadoFiltraPorLicenciaEstado() {
        Empresa empresa = empresaConId(EMPRESA_ID, "Mi Tienda", LicenciaEstado.SUSPENDIDA);
        when(empresaRepository.findByLicenciaEstado(LicenciaEstado.SUSPENDIDA)).thenReturn(List.of(empresa));
        when(usuarioRepository.findByEmpresaIdAndRolEmpresa(EMPRESA_ID, RolEmpresa.ADMIN)).thenReturn(List.of());

        List<EmpresaAdminDto> resultado = service.listar(LicenciaEstado.SUSPENDIDA);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).licenciaEstado()).isEqualTo(LicenciaEstado.SUSPENDIDA);
    }

    @Test
    void listarIncluyeNombreYEmailDelPrimerAdminDeCadaEmpresa() {
        Empresa empresa = empresaConId(EMPRESA_ID, "Mi Tienda", LicenciaEstado.ACTIVA);
        Usuario admin = usuario("Ana Admin", "ana@mitienda.com");
        when(empresaRepository.findAll()).thenReturn(List.of(empresa));
        when(usuarioRepository.findByEmpresaIdAndRolEmpresa(EMPRESA_ID, RolEmpresa.ADMIN)).thenReturn(List.of(admin));

        List<EmpresaAdminDto> resultado = service.listar(null);

        assertThat(resultado.get(0).adminNombre()).isEqualTo("Ana Admin");
        assertThat(resultado.get(0).adminEmail()).isEqualTo("ana@mitienda.com");
    }

    @Test
    void listarSinNingunAdminCargadoDejaEsosCamposEnNull() {
        Empresa empresa = empresaConId(EMPRESA_ID, "Mi Tienda", LicenciaEstado.ACTIVA);
        when(empresaRepository.findAll()).thenReturn(List.of(empresa));
        when(usuarioRepository.findByEmpresaIdAndRolEmpresa(EMPRESA_ID, RolEmpresa.ADMIN)).thenReturn(List.of());

        List<EmpresaAdminDto> resultado = service.listar(null);

        assertThat(resultado.get(0).adminNombre()).isNull();
        assertThat(resultado.get(0).adminEmail()).isNull();
    }

    // --- actualizarLicencia ---

    @Test
    void actualizarLicenciaCambiaEstadoYVencimientoYLosPersiste() {
        Empresa empresa = empresaConId(EMPRESA_ID, "Mi Tienda", LicenciaEstado.PENDIENTE);
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByEmpresaIdAndRolEmpresa(EMPRESA_ID, RolEmpresa.ADMIN)).thenReturn(List.of());

        LocalDate vencimiento = LocalDate.of(2027, 1, 1);
        ActualizarLicenciaRequest request = new ActualizarLicenciaRequest(LicenciaEstado.ACTIVA, vencimiento);

        EmpresaAdminDto resultado = service.actualizarLicencia(EMPRESA_ID, request);

        assertThat(resultado.licenciaEstado()).isEqualTo(LicenciaEstado.ACTIVA);
        assertThat(resultado.licenciaVencimiento()).isEqualTo(vencimiento);
        ArgumentCaptor<Empresa> captor = ArgumentCaptor.forClass(Empresa.class);
        org.mockito.Mockito.verify(empresaRepository).save(captor.capture());
        assertThat(captor.getValue().getLicenciaEstado()).isEqualTo(LicenciaEstado.ACTIVA);
        assertThat(captor.getValue().getLicenciaVencimiento()).isEqualTo(vencimiento);
    }

    @Test
    void actualizarLicenciaDeEmpresaInexistenteLanzaExcepcion() {
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.empty());

        ActualizarLicenciaRequest request = new ActualizarLicenciaRequest(LicenciaEstado.ACTIVA, null);

        assertThatThrownBy(() -> service.actualizarLicencia(EMPRESA_ID, request))
                .isInstanceOf(EmpresaNoEncontradaException.class);

        org.mockito.Mockito.verify(empresaRepository, org.mockito.Mockito.never()).save(any());
    }

    private Empresa empresaConId(Long id, String nombre, LicenciaEstado estado) {
        Empresa empresa = new Empresa();
        empresa.setId(id);
        empresa.setNombre(nombre);
        empresa.setLicenciaEstado(estado);
        return empresa;
    }

    private Usuario usuario(String nombre, String email) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        return usuario;
    }
}
