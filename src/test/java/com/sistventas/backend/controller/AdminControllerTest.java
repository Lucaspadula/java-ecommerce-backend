package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ActualizarLicenciaRequest;
import com.sistventas.backend.dto.EmpresaAdminDto;
import com.sistventas.backend.entity.LicenciaEstado;
import com.sistventas.backend.service.EmpresaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests del controller invocando los métodos directamente (sin MockMvc): la
// restricción a ROLE_SUPER_ADMIN vive en SecurityConfig, no acá — lo que
// interesa es que AdminController delegue en EmpresaService con los
// parámetros correctos y arme el ResponseEntity con el status esperado.
@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private EmpresaService empresaService;

    @InjectMocks
    private AdminController adminController;

    @Test
    void listarEmpresasSinFiltroDevuelveOkConLaListaDelService() {
        EmpresaAdminDto dto = empresaDto(1L, "Mi Empresa", LicenciaEstado.ACTIVA);
        when(empresaService.listar(null)).thenReturn(List.of(dto));

        ResponseEntity<List<EmpresaAdminDto>> respuesta = adminController.listarEmpresas(null);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(empresaService).listar(null);
    }

    @Test
    void listarEmpresasConFiltroDeEstadoDelegaElFiltroAlService() {
        EmpresaAdminDto dto = empresaDto(2L, "Otra Empresa", LicenciaEstado.VENCIDA);
        when(empresaService.listar(LicenciaEstado.VENCIDA)).thenReturn(List.of(dto));

        ResponseEntity<List<EmpresaAdminDto>> respuesta = adminController.listarEmpresas(LicenciaEstado.VENCIDA);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(dto);
        verify(empresaService).listar(LicenciaEstado.VENCIDA);
    }

    @Test
    void actualizarLicenciaDevuelveOkConLaEmpresaActualizada() {
        ActualizarLicenciaRequest request = new ActualizarLicenciaRequest(LicenciaEstado.ACTIVA, LocalDate.of(2027, 1, 1));
        EmpresaAdminDto dto = empresaDto(1L, "Mi Empresa", LicenciaEstado.ACTIVA);
        when(empresaService.actualizarLicencia(1L, request)).thenReturn(dto);

        ResponseEntity<EmpresaAdminDto> respuesta = adminController.actualizarLicencia(1L, request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(empresaService).actualizarLicencia(1L, request);
    }

    private EmpresaAdminDto empresaDto(Long id, String nombre, LicenciaEstado estado) {
        return new EmpresaAdminDto(id, nombre, estado, LocalDate.now(), LocalDateTime.now(), "Admin", "admin@test.com");
    }
}
