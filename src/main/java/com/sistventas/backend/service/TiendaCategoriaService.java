package com.sistventas.backend.service;

import com.sistventas.backend.dto.ActualizarColorCategoriaRequest;
import com.sistventas.backend.dto.CategoriaTiendaDto;
import com.sistventas.backend.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Solo ADMIN (ver adminEmpresaIdOrThrow en la impl). Todos los métodos de
// escritura son upsert por (empresaId, categoriaId): puede no existir
// todavía una fila en tienda_categoria la primera vez que se personaliza esa
// categoría.
public interface TiendaCategoriaService {
    List<CategoriaTiendaDto> listar(UserPrincipal principal);

    CategoriaTiendaDto actualizarColor(Long categoriaId, ActualizarColorCategoriaRequest request, UserPrincipal principal);

    CategoriaTiendaDto actualizarImagen(Long categoriaId, MultipartFile file, UserPrincipal principal);

    CategoriaTiendaDto eliminarImagen(Long categoriaId, UserPrincipal principal);
}
