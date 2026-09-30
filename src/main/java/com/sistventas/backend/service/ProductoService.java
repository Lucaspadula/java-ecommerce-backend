package com.sistventas.backend.service;

import com.sistventas.backend.dto.ActualizarProductoRequest;
import com.sistventas.backend.dto.AjustePrecioCategoriaRequest;
import com.sistventas.backend.dto.AjustePrecioCategoriaResultadoDto;
import com.sistventas.backend.dto.CrearResenaRequest;
import com.sistventas.backend.dto.FotoUploadDto;
import com.sistventas.backend.dto.ProductoDto;
import com.sistventas.backend.dto.ProductoRequest;
import com.sistventas.backend.dto.ResenaDto;
import com.sistventas.backend.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductoService {
    List<ProductoDto> listar(UserPrincipal principal);

    ProductoDto obtener(Long id, UserPrincipal principal);

    ProductoDto crear(ProductoRequest request, UserPrincipal principal);

    ProductoDto actualizar(Long id, ActualizarProductoRequest request, UserPrincipal principal);

    void eliminar(Long id, UserPrincipal principal);

    // Pool unificado de fotos (V43__producto_foto_pool.sql, reemplaza el
    // viejo esquema de slots fijos 1-4): hasta 8 por producto, cada una con
    // varianteId opcional (color). Valida el tope 8 ANTES de escribir el
    // archivo a disco (ver design Open Question).
    ProductoDto agregarFoto(Long id, MultipartFile file, Long varianteId, UserPrincipal principal);

    // Borra una foto puntual del pool por su id: pone el archivo del disco
    // best-effort (mismo criterio que el resto del proyecto, ver
    // PerfilServiceImpl/TiendaCategoriaServiceImpl) y reindexa el `orden` de
    // las restantes a 0..n-1. 404 si la foto no pertenece a este producto.
    ProductoDto eliminarFoto(Long id, Long fotoId, UserPrincipal principal);

    // Reasigna `orden` 0..n-1 según la posición de cada id en fotoIds. 404 si
    // la lista contiene una foto que no pertenece a este producto.
    ProductoDto reordenarFotos(Long id, List<Long> fotoIds, UserPrincipal principal);

    // Cambia el variante_id de una foto sin re-subir el archivo (ver spec
    // "Cambio de color sin re-subida"). varianteId null = quita el color
    // (vuelve a "general").
    ProductoDto asignarColorFoto(Long id, Long fotoId, Long varianteId, UserPrincipal principal);

    // Toggle manual del ajuste de una foto (ver spec "Ajuste manual de
    // foto"): false = se ve completa (contain), true = se agranda llenando
    // el marco aunque recorte bordes (cover).
    ProductoDto ajustarFoto(Long id, Long fotoId, boolean agrandada, UserPrincipal principal);

    AjustePrecioCategoriaResultadoDto ajustarPrecioPorCategoria(AjustePrecioCategoriaRequest request, UserPrincipal principal);

    // Upload genérico, no atado a un producto/variante puntual — mismo
    // criterio que VentaService.subirFoto: el frontend sube la foto mientras
    // arma el form (variante nueva o existente) y guarda la fotoUrl devuelta
    // en la fila correspondiente, para mandarla como parte del payload
    // normal de POST/PUT /api/productos.
    FotoUploadDto subirFotoVariante(MultipartFile file, UserPrincipal principal);

    // Testimonios de clientes que el dueño carga a mano desde el panel de
    // administración (ver Resena) — viven en el contexto de un producto, así
    // que reusan el mismo scoping por empresa que el resto de este service.
    List<ResenaDto> listarResenas(Long productoId, UserPrincipal principal);

    ResenaDto crearResena(Long productoId, CrearResenaRequest request, UserPrincipal principal);

    void eliminarResena(Long productoId, Long resenaId, UserPrincipal principal);

    // Captura de WhatsApp opcional como respaldo visual de la reseña, subida
    // aparte del alta (JSON) — mismo patrón multipart que actualizarFoto.
    ResenaDto actualizarFotoResena(Long productoId, Long resenaId, MultipartFile file, UserPrincipal principal);
}
