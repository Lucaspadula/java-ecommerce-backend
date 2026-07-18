package com.sistventas.backend.service;

import com.sistventas.backend.dto.ActualizarEstadoVentaRequest;
import com.sistventas.backend.dto.FotoUploadDto;
import com.sistventas.backend.dto.TextoCompartirDto;
import com.sistventas.backend.dto.VentaDto;
import com.sistventas.backend.dto.VentaEstadoHistorialDto;
import com.sistventas.backend.dto.VentaRequest;
import com.sistventas.backend.entity.EstadoVenta;
import com.sistventas.backend.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VentaService {
    List<VentaDto> listar(EstadoVenta estado, Long clienteId, UserPrincipal principal);

    List<VentaEstadoHistorialDto> historialEstados(Long id, UserPrincipal principal);

    VentaDto obtener(Long id, UserPrincipal principal);

    VentaDto crear(VentaRequest request, UserPrincipal principal);

    VentaDto actualizar(Long id, VentaRequest request, UserPrincipal principal);

    VentaDto actualizarEstado(Long id, ActualizarEstadoVentaRequest request, UserPrincipal principal);

    void eliminar(Long id, UserPrincipal principal);

    TextoCompartirDto textoCompartir(Long id, UserPrincipal principal);

    // Upload genérico, no atado a ningún item/venta: los items no tienen id
    // estable hasta que la venta se guarda (aplicarDatos los reconstruye
    // enteros en cada crear/actualizar). El frontend sube la foto mientras
    // arma el formulario y manda la url resultante como parte del payload.
    FotoUploadDto subirFoto(MultipartFile file, UserPrincipal principal);
}
