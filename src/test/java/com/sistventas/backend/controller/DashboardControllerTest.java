package com.sistventas.backend.controller;

import com.sistventas.backend.dto.DashboardResumenDto;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests del controller invocando los métodos directamente (sin MockMvc): lo
// que interesa acá es que DashboardController delegue en el service con el
// principal correcto y arme el ResponseEntity con el status HTTP esperado,
// no simular el dispatcher HTTP completo ni @AuthenticationPrincipal.
@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void resumenDevuelveOkConElResumenDelServiceScopeadoAlPrincipal() {
        DashboardResumenDto dto = new DashboardResumenDto(
                BigDecimal.valueOf(1000), BigDecimal.valueOf(300), BigDecimal.valueOf(30),
                List.of(), 5L, 10L, 2L,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        when(dashboardService.resumen(principal)).thenReturn(dto);

        ResponseEntity<DashboardResumenDto> respuesta = dashboardController.resumen(principal);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
        verify(dashboardService).resumen(principal);
    }
}
