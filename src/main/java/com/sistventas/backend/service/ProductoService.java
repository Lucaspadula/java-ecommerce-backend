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

    // slot 1/2/3 determina si se actualiza fotoUrl, fotoUrl2 o fotoUrl3 (ver
    // Producto). Valida rango 1-3 con ArchivoInvalidoException (400).
    ProductoDto actualizarFoto(Long id, MultipartFile file, int slot, UserPrincipal principal);

    // Borra la foto de un slot puntual sin subir una nueva: pone el campo en
    // null y borra el archivo del disco best-effort (mismo criterio que el
    // resto del proyecto, ver PerfilServiceImpl/TiendaCategoriaServiceImpl).
    ProductoDto eliminarFoto(Long id, int slot, UserPrincipal principal);

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
