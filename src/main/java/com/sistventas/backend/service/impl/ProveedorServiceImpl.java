package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ProveedorDto;
import com.sistventas.backend.dto.ProveedorRequest;
import com.sistventas.backend.entity.EstadoPedido;
import com.sistventas.backend.entity.Proveedor;
import com.sistventas.backend.exception.ProveedorNoEncontradoException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.ProveedorRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.ProveedorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorServiceImpl(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorDto> listar(UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        return proveedorRepository.findByEmpresaIdAndActivoTrue(empresaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorDto obtener(Long id, UserPrincipal principal) {
        return toDto(buscarPorEmpresa(id, principal));
    }

    @Override
    @Transactional
    public ProveedorDto crear(ProveedorRequest request, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);

        Proveedor proveedor = new Proveedor();
        proveedor.setEmpresaId(empresaId);
        proveedor.setActivo(true);
        proveedor.setFechaAlta(LocalDateTime.now());
        aplicarDatos(proveedor, request);

        return toDto(proveedorRepository.save(proveedor));
    }

    @Override
    @Transactional
    public ProveedorDto actualizar(Long id, ProveedorRequest request, UserPrincipal principal) {
        Proveedor proveedor = buscarPorEmpresa(id, principal);
        aplicarDatos(proveedor, request);
        return toDto(proveedorRepository.save(proveedor));
    }

    @Override
    @Transactional
    public void eliminar(Long id, UserPrincipal principal) {
        Proveedor proveedor = buscarPorEmpresa(id, principal);
        proveedor.setActivo(false);
        proveedorRepository.save(proveedor);
    }

    private void aplicarDatos(Proveedor proveedor, ProveedorRequest request) {
        proveedor.setNombre(request.nombre());
        proveedor.setContacto(request.contacto());
        proveedor.setNotas(request.notas());
        // Si no viene estado explícito, se deja SIN_PEDIDO en vez de dejar la
        // columna en null (es NOT NULL en la base).
        proveedor.setEstadoPedido(request.estadoPedido() != null ? request.estadoPedido() : EstadoPedido.SIN_PEDIDO);
        proveedor.setDetallePedidoActual(request.detallePedidoActual());
        proveedor.setFechaPedido(request.fechaPedido());
        proveedor.setFechaLlegadaEstimada(request.fechaLlegadaEstimada());
        proveedor.setUltimoPedidoDetalle(request.ultimoPedidoDetalle());
    }

    private Proveedor buscarPorEmpresa(Long id, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        return proveedorRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(ProveedorNoEncontradoException::new);
    }

    // Único punto donde se resuelve empresaId del usuario logueado. Nunca se
    // acepta un empresaId del cliente (body/query) — siempre sale del
    // UserPrincipal armado en JwtAuthenticationFilter a partir del JWT.
    private Long empresaIdOrThrow(UserPrincipal principal) {
        if (principal == null || principal.empresaId() == null) {
            throw new SinEmpresaException();
        }
        return principal.empresaId();
    }

    private ProveedorDto toDto(Proveedor proveedor) {
        return new ProveedorDto(
                proveedor.getId(),
                proveedor.getNombre(),
                proveedor.getContacto(),
                proveedor.getNotas(),
                proveedor.getEstadoPedido(),
                proveedor.getDetallePedidoActual(),
                proveedor.getFechaPedido(),
                proveedor.getFechaLlegadaEstimada(),
                proveedor.getUltimoPedidoDetalle(),
                proveedor.getFechaAlta()
        );
    }
}
