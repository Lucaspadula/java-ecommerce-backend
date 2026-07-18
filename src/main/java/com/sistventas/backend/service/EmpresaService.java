package com.sistventas.backend.service;

import com.sistventas.backend.dto.ActualizarLicenciaRequest;
import com.sistventas.backend.dto.EmpresaAdminDto;
import com.sistventas.backend.entity.LicenciaEstado;

import java.util.List;

public interface EmpresaService {

    List<EmpresaAdminDto> listar(LicenciaEstado estado);

    EmpresaAdminDto actualizarLicencia(Long id, ActualizarLicenciaRequest request);
}
