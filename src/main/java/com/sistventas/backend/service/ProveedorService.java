package com.sistventas.backend.service;

import com.sistventas.backend.dto.ProveedorDto;
import com.sistventas.backend.dto.ProveedorRequest;
import com.sistventas.backend.security.UserPrincipal;

import java.util.List;

public interface ProveedorService {
    List<ProveedorDto> listar(UserPrincipal principal);

    ProveedorDto obtener(Long id, UserPrincipal principal);

    ProveedorDto crear(ProveedorRequest request, UserPrincipal principal);

    ProveedorDto actualizar(Long id, ProveedorRequest request, UserPrincipal principal);

    void eliminar(Long id, UserPrincipal principal);
}
