package com.sistventas.backend.service;

import com.sistventas.backend.dto.ClienteDto;
import com.sistventas.backend.dto.ClienteRequest;
import com.sistventas.backend.security.UserPrincipal;

import java.util.List;

public interface ClienteService {
    List<ClienteDto> listar(UserPrincipal principal);

    // Los que quedaron con activo=false tras eliminar() — para poder
    // deshacer un borrado por error sin tocar la base a mano.
    List<ClienteDto> listarInactivos(UserPrincipal principal);

    ClienteDto obtener(Long id, UserPrincipal principal);

    ClienteDto crear(ClienteRequest request, UserPrincipal principal);

    ClienteDto actualizar(Long id, ClienteRequest request, UserPrincipal principal);

    void eliminar(Long id, UserPrincipal principal);

    ClienteDto restaurar(Long id, UserPrincipal principal);
}
