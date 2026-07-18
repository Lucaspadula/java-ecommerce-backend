package com.sistventas.backend.controller;

import com.sistventas.backend.dto.ActualizarLicenciaRequest;
import com.sistventas.backend.dto.EmpresaAdminDto;
import com.sistventas.backend.entity.LicenciaEstado;
import com.sistventas.backend.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Backoffice del Super Admin: alta/gestión de empresas y su estado de
 * licencia. Restringido por SecurityConfig a la authority ROLE_SUPER_ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final EmpresaService empresaService;

    public AdminController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping("/empresas")
    public ResponseEntity<List<EmpresaAdminDto>> listarEmpresas(
            @RequestParam(required = false) LicenciaEstado estado) {
        return ResponseEntity.ok(empresaService.listar(estado));
    }

    @PatchMapping("/empresas/{id}/licencia")
    public ResponseEntity<EmpresaAdminDto> actualizarLicencia(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarLicenciaRequest request) {
        return ResponseEntity.ok(empresaService.actualizarLicencia(id, request));
    }
}
