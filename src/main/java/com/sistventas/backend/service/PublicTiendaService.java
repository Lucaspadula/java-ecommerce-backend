package com.sistventas.backend.service;

import com.sistventas.backend.dto.ActualizarPerfilClienteRequest;
import com.sistventas.backend.dto.AtributoFiltroDto;
import com.sistventas.backend.dto.CategoriaTiendaDto;
import com.sistventas.backend.dto.ClienteLoginDto;
import com.sistventas.backend.dto.ClienteLoginGoogleRequest;
import com.sistventas.backend.dto.ClienteLoginRequest;
import com.sistventas.backend.dto.ClienteLoginResponse;
import com.sistventas.backend.dto.CrearResenaClienteRequest;
import com.sistventas.backend.dto.RegistrarClienteGoogleRequest;
import com.sistventas.backend.dto.RegistrarClienteRequest;
import com.sistventas.backend.dto.FotoUploadDto;
import com.sistventas.backend.dto.PreviewDescuentoComboDto;
import com.sistventas.backend.dto.PreviewDescuentoComboRequest;
import com.sistventas.backend.dto.PublicCategoriaMenuDto;
import com.sistventas.backend.dto.PublicEmpresaDto;
import com.sistventas.backend.dto.PublicPedidoEstadoDto;
import com.sistventas.backend.dto.PublicPedidoRequest;
import com.sistventas.backend.dto.PublicPedidoResultadoDto;
import com.sistventas.backend.dto.PublicProductoDto;
import com.sistventas.backend.dto.PublicTipDto;
import com.sistventas.backend.dto.ResenaDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Sin UserPrincipal en ningún método: la vidriera pública no tiene JWT, la
// empresa se resuelve siempre a partir del slug de la URL.
public interface PublicTiendaService {
    PublicEmpresaDto obtenerEmpresa(String slug);

    // Sube la imagen del grabado (logo/diseño) ANTES de armar el pedido — el
    // cliente manda la URL resultante en PublicPedidoItemRequest.grabadoImagenUrl.
    FotoUploadDto subirFotoGrabado(String slug, MultipartFile file);

    List<PublicProductoDto> listarProductos(String slug);

    // Solo lectura: reusa ResenaDto (id, clienteNombre, comentario, fecha) —
    // no tiene ningún dato sensible de costo, así que no hace falta un DTO
    // público separado como sí existe para producto (ver PublicProductoDto).
    List<ResenaDto> listarResenas(String slug, Long productoId);

    // Reseña de un cliente real con compra verificada, sin login: valida
    // venta + teléfono (mismo criterio que consultarPedido), que esté
    // ENTREGADA, que el producto esté entre sus items, y que no haya ya una
    // reseña de esa misma compra — ver PublicTiendaServiceImpl.crearResenaCliente.
    ResenaDto crearResenaCliente(String slug, Long productoId, CrearResenaClienteRequest request);

    // Login de cuenta de cliente por email+contraseña. Fase 1: sin registro
    // todavía — si no existe cuenta, tira el mismo error genérico que un
    // password incorrecto (CredencialesInvalidasException), nunca un 404
    // que revele si ese email existe.
    ClienteLoginResponse loginCliente(String slug, ClienteLoginRequest request);

    // Login de cuenta de cliente con Google — vincula automáticamente si ya
    // existe un Cliente con ese email (ver PublicTiendaServiceImpl).
    ClienteLoginResponse loginClienteGoogle(String slug, ClienteLoginGoogleRequest request);

    // Registro manual: si el teléfono o el email ya matchean un Cliente sin
    // credenciales (de una compra de invitado), el registro se adosa a esa
    // fila en vez de crear una duplicada — ver PublicTiendaServiceImpl.
    ClienteLoginResponse registrarCliente(String slug, RegistrarClienteRequest request);

    // Registro con Google: pide el teléfono en un paso extra (el idToken no
    // lo trae) para poder aplicar el mismo criterio de vínculo con compras.
    ClienteLoginResponse registrarClienteGoogle(String slug, RegistrarClienteGoogleRequest request);

    // Tips de cuidado del mate (título + contenido, sin autor asociado):
    // franja fija en la home de la tienda pública, mostrada después de los
    // testimonios, cargados a mano por el dueño desde el panel admin.
    List<PublicTipDto> listarTips(String slug);

    // Solo las categorías reales de productos activos que además tienen
    // color y/o imagen configurados (ver TiendaCategoriaServiceImpl para el
    // endpoint de administración, que sí devuelve TODAS las categorías
    // reales estén o no configuradas).
    List<CategoriaTiendaDto> listarCategorias(String slug);

    // Árbol categoría -> subcategorías para el mega-menú del navbar (ver
    // PublicTiendaServiceImpl.listarCategoriasMenu) — a diferencia de
    // listarCategorias, incluye TODAS las categorías con productos activos
    // (tengan o no imagen configurada), porque acá es navegación real, no
    // una vidriera decorativa.
    List<PublicCategoriaMenuDto> listarCategoriasMenu(String slug);

    // Atributos de filtro definidos por el dueño para esta categoría (ver
    // CategoriaService.listarAtributosFiltro, versión admin) — la pantalla de
    // categoría de la tienda pública los usa para agrupar sus chips de
    // filtro por nombre de atributo (ej. "Material"), algo que el propio
    // PublicProductoDto.atributoValores no puede: viaja plano, sin ese
    // nombre de grupo.
    List<AtributoFiltroDto> listarAtributosFiltro(String slug, Long categoriaId);

    PublicPedidoResultadoDto crearPedido(String slug, PublicPedidoRequest request);

    PublicPedidoEstadoDto consultarPedido(String slug, Long ventaId, String telefono);

    // "Mis pedidos" de la cuenta de cliente logueada — a diferencia de
    // consultarPedido (busca UNA venta por id+teléfono, sin sesión), acá el
    // cliente ya está autenticado por JWT (ver ClientePrincipal) y se listan
    // TODAS sus ventas de esta empresa, más recientes primero.
    List<PublicPedidoEstadoDto> listarPedidosCliente(String slug, Long clienteId);

    // "Mis datos" editable de la cuenta de cliente logueada — nombre/teléfono
    // nada más (sin email/password, eso no se edita acá).
    ClienteLoginDto actualizarPerfilCliente(String slug, Long clienteId, ActualizarPerfilClienteRequest request);

    // Preview del descuento combo para el carrito, ANTES de confirmar el
    // pedido: recalcula lo mismo que crearPedido (nunca confía en precio ni
    // categoría que venga del cliente), pero no persiste nada.
    PreviewDescuentoComboDto previewDescuentoCombo(String slug, PreviewDescuentoComboRequest request);
}
