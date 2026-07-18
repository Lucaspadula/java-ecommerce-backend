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
import com.sistventas.backend.service.EmpresaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    public EmpresaServiceImpl(EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmpresaAdminDto> listar(LicenciaEstado estado) {
        List<Empresa> empresas = estado != null
                ? empresaRepository.findByLicenciaEstado(estado)
                : empresaRepository.findAll();

        return empresas.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public EmpresaAdminDto actualizarLicencia(Long id, ActualizarLicenciaRequest request) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(EmpresaNoEncontradaException::new);

        empresa.setLicenciaEstado(request.estado());
        empresa.setLicenciaVencimiento(request.vencimiento());
        empresa = empresaRepository.save(empresa);

        return toDto(empresa);
    }

    private EmpresaAdminDto toDto(Empresa empresa) {
        Usuario admin = usuarioRepository.findByEmpresaIdAndRolEmpresa(empresa.getId(), RolEmpresa.ADMIN)
                .stream()
                .findFirst()
                .orElse(null);

        return new EmpresaAdminDto(
                empresa.getId(),
                empresa.getNombre(),
                empresa.getLicenciaEstado(),
                empresa.getLicenciaVencimiento(),
                empresa.getFechaAlta(),
                admin != null ? admin.getNombre() : null,
                admin != null ? admin.getEmail() : null
        );
    }
}
