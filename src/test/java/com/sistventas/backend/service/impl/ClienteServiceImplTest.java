package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ClienteDto;
import com.sistventas.backend.dto.ClienteRequest;
import com.sistventas.backend.entity.Cliente;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.exception.AccionNoPermitidaException;
import com.sistventas.backend.repository.ClienteRepository;
import com.sistventas.backend.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests de las reglas agregadas a ClienteServiceImpl: nombre único
// case-insensitive por empresa (con el propio registro excluido al editar),
// restaurar() sobre un cliente inactivo y listarInactivos(). El repository
// se mockea entero: acá no interesa JPA, interesa la lógica de negocio.
@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void crearConNombreYaExistenteActivoLanzaExcepcion() {
        ClienteRequest request = new ClienteRequest("Juan", null, null, null);
        when(clienteRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrue(EMPRESA_ID, "Juan"))
                .thenReturn(true);

        assertThatThrownBy(() -> clienteService.crear(request, principal))
                .isInstanceOf(AccionNoPermitidaException.class);

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void crearConNombreLibrePersiste() {
        ClienteRequest request = new ClienteRequest("Pedro", null, null, null);
        when(clienteRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrue(EMPRESA_ID, "Pedro"))
                .thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClienteDto resultado = clienteService.crear(request, principal);

        assertThat(resultado.nombre()).isEqualTo("Pedro");
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    void actualizarConNombreDeOtroClienteLanzaExcepcion() {
        Cliente existente = cliente(5L, "Ana");
        when(clienteRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrueAndIdNot(EMPRESA_ID, "Maria", 5L))
                .thenReturn(true);

        ClienteRequest request = new ClienteRequest("Maria", null, null, null);

        assertThatThrownBy(() -> clienteService.actualizar(5L, request, principal))
                .isInstanceOf(AccionNoPermitidaException.class);

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void actualizarConSuPropioNombreNoChocaContraSiMismo() {
        Cliente existente = cliente(5L, "Ana");
        when(clienteRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrueAndIdNot(EMPRESA_ID, "Ana", 5L))
                .thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClienteRequest request = new ClienteRequest("Ana", null, null, null);

        ClienteDto resultado = clienteService.actualizar(5L, request, principal);

        assertThat(resultado.nombre()).isEqualTo("Ana");
        verify(clienteRepository).existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrueAndIdNot(EMPRESA_ID, "Ana", 5L);
        verify(clienteRepository, never())
                .existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrue(anyLong(), eq("Ana"));
    }

    @Test
    void restaurarEncuentraClienteInactivoLoActivaYLoGuarda() {
        Cliente inactivo = cliente(7L, "Luis");
        inactivo.setActivo(false);
        when(clienteRepository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.of(inactivo));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClienteDto resultado = clienteService.restaurar(7L, principal);

        assertThat(inactivo.isActivo()).isTrue();
        assertThat(resultado.nombre()).isEqualTo("Luis");
        verify(clienteRepository).save(inactivo);
    }

    @Test
    void listarInactivosDelegaEnElRepositoryYMapeaADto() {
        Cliente inactivo = cliente(8L, "Sofia");
        inactivo.setActivo(false);
        when(clienteRepository.findByEmpresaIdAndActivoFalse(EMPRESA_ID)).thenReturn(List.of(inactivo));

        List<ClienteDto> resultado = clienteService.listarInactivos(principal);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Sofia");
    }

    private Cliente cliente(Long id, String nombre) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setEmpresaId(EMPRESA_ID);
        cliente.setNombre(nombre);
        cliente.setActivo(true);
        return cliente;
    }
}
