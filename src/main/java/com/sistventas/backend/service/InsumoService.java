package com.sistventas.backend.service;

import com.sistventas.backend.dto.ImportarInsumosResultadoDto;
import com.sistventas.backend.dto.InsumoDto;
import com.sistventas.backend.dto.InsumoRequest;
import com.sistventas.backend.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InsumoService {
    List<InsumoDto> listar(UserPrincipal principal);

    // Los que quedaron con activo=false tras eliminar() — para poder
    // deshacer un borrado por error sin tocar la base a mano.
    List<InsumoDto> listarInactivos(UserPrincipal principal);

    InsumoDto obtener(Long id, UserPrincipal principal);

    InsumoDto crear(InsumoRequest request, UserPrincipal principal);

    InsumoDto actualizar(Long id, InsumoRequest request, UserPrincipal principal);

    void eliminar(Long id, UserPrincipal principal);

    InsumoDto restaurar(Long id, UserPrincipal principal);

    byte[] generarPlantilla();

    ImportarInsumosResultadoDto importarDesdeExcel(MultipartFile file, UserPrincipal principal);
}
