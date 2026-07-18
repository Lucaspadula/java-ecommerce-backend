package com.sistventas.backend.controller;

import com.sistventas.backend.dto.DashboardResumenDto;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * KPIs agregados del dashboard, scopeados a la empresa del usuario logueado.
 * Mismo criterio multiempresa que el resto de los controllers: el scoping
 * sale siempre del UserPrincipal, nunca de un parámetro del cliente.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumen")
    public ResponseEntity<DashboardResumenDto> resumen(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dashboardService.resumen(principal));
    }
}
