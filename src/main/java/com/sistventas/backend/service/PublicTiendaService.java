package com.sistventas.backend.service;

import com.sistventas.backend.dto.CategoriaTiendaDto;
import com.sistventas.backend.dto.PreviewDescuentoComboDto;
import com.sistventas.backend.dto.PreviewDescuentoComboRequest;
import com.sistventas.backend.dto.PublicEmpresaDto;
import com.sistventas.backend.dto.PublicPedidoEstadoDto;
import com.sistventas.backend.dto.PublicPedidoRequest;
import com.sistventas.backend.dto.PublicPedidoResultadoDto;
import com.sistventas.backend.dto.PublicProductoDto;
import com.sistventas.backend.dto.PublicTestimonioDto;
import com.sistventas.backend.dto.PublicTipDto;
import com.sistventas.backend.dto.ResenaDto;

import java.util.List;

// Sin UserPrincipal en ningún método: la vidriera pública no tiene JWT, la
// empresa se resuelve siempre a partir del slug de la URL.
public interface PublicTiendaService {
    PublicEmpresaDto obtenerEmpresa(String slug);

    List<PublicProductoDto> listarProductos(String slug);

    // Solo lectura: reusa ResenaDto (id, clienteNombre, comentario, fecha) —
    // no tiene ningún dato sensible de costo, así que no hace falta un DTO
    // público separado como sí existe para producto (ver PublicProductoDto).
    List<ResenaDto> listarResenas(String slug, Long productoId);

    // Testimonios generales del negocio (no ligados a un producto puntual,
    // a diferencia de listarResenas): franja fija en la home de la tienda
    // pública, cargados a mano por el dueño desde el panel admin.
    List<PublicTestimonioDto> listarTestimonios(String slug);

    // Tips de cuidado del mate (título + contenido, sin autor asociado):
    // franja fija en la home de la tienda pública, mostrada después de los
    // testimonios, cargados a mano por el dueño desde el panel admin.
    List<PublicTipDto> listarTips(String slug);

    // Solo las categorías reales de productos activos que además tienen
    // color y/o imagen configurados (ver TiendaCategoriaServiceImpl para el
    // endpoint de administración, que sí devuelve TODAS las categorías
    // reales estén o no configuradas).
    List<CategoriaTiendaDto> listarCategorias(String slug);

    PublicPedidoResultadoDto crearPedido(String slug, PublicPedidoRequest request);

    PublicPedidoEstadoDto consultarPedido(String slug, Long ventaId, String telefono);

    // Preview del descuento combo para el carrito, ANTES de confirmar el
    // pedido: recalcula lo mismo que crearPedido (nunca confía en precio ni
    // categoría que venga del cliente), pero no persiste nada.
    PreviewDescuentoComboDto previewDescuentoCombo(String slug, PreviewDescuentoComboRequest request);
}
