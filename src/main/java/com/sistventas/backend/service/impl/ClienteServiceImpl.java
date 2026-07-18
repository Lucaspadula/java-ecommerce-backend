package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ClienteDto;
import com.sistventas.backend.dto.ClienteRequest;
import com.sistventas.backend.entity.Cliente;
import com.sistventas.backend.exception.ClienteNoEncontradoException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.ClienteRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.ClienteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteDto> listar(UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        return clienteRepository.findByEmpresaIdAndActivoTrue(empresaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteDto obtener(Long id, UserPrincipal principal) {
        return toDto(buscarPorEmpresa(id, principal));
    }

    @Override
    @Transactional
    public ClienteDto crear(ClienteRequest request, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);

        Cliente cliente = new Cliente();
        cliente.setEmpresaId(empresaId);
        cliente.setActivo(true);
        cliente.setFechaAlta(LocalDateTime.now());
        aplicarDatos(cliente, request);

        return toDto(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public ClienteDto actualizar(Long id, ClienteRequest request, UserPrincipal principal) {
        Cliente cliente = buscarPorEmpresa(id, principal);
        aplicarDatos(cliente, request);
        return toDto(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public void eliminar(Long id, UserPrincipal principal) {
        Cliente cliente = buscarPorEmpresa(id, principal);
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    private void aplicarDatos(Cliente cliente, ClienteRequest request) {
        cliente.setNombre(request.nombre());
        cliente.setEmail(request.email());
        cliente.setTelefono(request.telefono());
        cliente.setNotas(request.notas());
    }

    private Cliente buscarPorEmpresa(Long id, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        return clienteRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(ClienteNoEncontradoException::new);
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

    private ClienteDto toDto(Cliente cliente) {
        return new ClienteDto(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getNotas(),
                cliente.getFechaAlta()
        );
    }
}
