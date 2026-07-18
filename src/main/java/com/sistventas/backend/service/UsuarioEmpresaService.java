package com.sistventas.backend.service;

import com.sistventas.backend.dto.ActualizarActivoRequest;
import com.sistventas.backend.dto.InvitarUsuarioRequest;
import com.sistventas.backend.dto.UsuarioEmpresaDto;
import com.sistventas.backend.security.UserPrincipal;

import java.util.List;

public interface UsuarioEmpresaService {
    List<UsuarioEmpresaDto> listar(UserPrincipal principal);

    UsuarioEmpresaDto invitar(InvitarUsuarioRequest request, UserPrincipal principal);

    UsuarioEmpresaDto actualizarActivo(Long id, ActualizarActivoRequest request, UserPrincipal principal);
}
