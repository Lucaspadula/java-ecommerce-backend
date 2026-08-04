package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ProveedorDto;
import com.sistventas.backend.dto.ProveedorRequest;
import com.sistventas.backend.entity.EstadoPedido;
import com.sistventas.backend.entity.Proveedor;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.exception.AccionNoPermitidaException;
import com.sistventas.backend.repository.ProveedorRepository;
import com.sistventas.backend.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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

// Mismo patrón de nombre único / restaurar / listarInactivos que
// Cliente/InsumoServiceImplTest, más el caso propio de Proveedor: el
// auto-clear de los campos de pedido en curso cuando el estado pasa A
// RECIBIDO (ver aplicarDatos() en ProveedorServiceImpl).
@ExtendWith(MockitoExtension.class)
class ProveedorServiceImplTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private ProveedorRepository proveedorRepository;

    @InjectMocks
    private ProveedorServiceImpl proveedorService;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void crearConNombreYaExistenteActivoLanzaExcepcion() {
        when(proveedorRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrue(EMPRESA_ID, "Maderera Sur"))
                .thenReturn(true);

        assertThatThrownBy(() -> proveedorService.crear(nombreRequest("Maderera Sur"), principal))
                .isInstanceOf(AccionNoPermitidaException.class);

        verify(proveedorRepository, never()).save(any());
    }

    @Test
    void crearConNombreLibrePersiste() {
        when(proveedorRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrue(EMPRESA_ID, "Cueros SRL"))
                .thenReturn(false);
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProveedorDto resultado = proveedorService.crear(nombreRequest("Cueros SRL"), principal);

        assertThat(resultado.nombre()).isEqualTo("Cueros SRL");
        verify(proveedorRepository).save(any(Proveedor.class));
    }

    @Test
    void actualizarConNombreDeOtroProveedorLanzaExcepcion() {
        Proveedor existente = proveedor(5L, "Insumos del Norte");
        when(proveedorRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(proveedorRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrueAndIdNot(EMPRESA_ID, "Textiles SA", 5L))
                .thenReturn(true);

        assertThatThrownBy(() -> proveedorService.actualizar(5L, nombreRequest("Textiles SA"), principal))
                .isInstanceOf(AccionNoPermitidaException.class);

        verify(proveedorRepository, never()).save(any());
    }

    @Test
    void actualizarConSuPropioNombreNoChocaContraSiMismo() {
        Proveedor existente = proveedor(5L, "Insumos del Norte");
        when(proveedorRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(proveedorRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrueAndIdNot(EMPRESA_ID, "Insumos del Norte", 5L))
                .thenReturn(false);
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProveedorDto resultado = proveedorService.actualizar(5L, nombreRequest("Insumos del Norte"), principal);

        assertThat(resultado.nombre()).isEqualTo("Insumos del Norte");
        verify(proveedorRepository)
                .existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrueAndIdNot(EMPRESA_ID, "Insumos del Norte", 5L);
        verify(proveedorRepository, never())
                .existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrue(anyLong(), eq("Insumos del Norte"));
    }

    @Test
    void restaurarEncuentraProveedorInactivoLoActivaYLoGuarda() {
        Proveedor inactivo = proveedor(7L, "Herrajes Bento");
        inactivo.setActivo(false);
        when(proveedorRepository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.of(inactivo));
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProveedorDto resultado = proveedorService.restaurar(7L, principal);

        assertThat(inactivo.isActivo()).isTrue();
        assertThat(resultado.nombre()).isEqualTo("Herrajes Bento");
        verify(proveedorRepository).save(inactivo);
    }

    @Test
    void listarInactivosDelegaEnElRepositoryYMapeaADto() {
        Proveedor inactivo = proveedor(8L, "Barnices Sol");
        inactivo.setActivo(false);
        when(proveedorRepository.findByEmpresaIdAndActivoFalse(EMPRESA_ID)).thenReturn(List.of(inactivo));

        List<ProveedorDto> resultado = proveedorService.listarInactivos(principal);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Barnices Sol");
    }

    // --- Auto-clear al pasar a RECIBIDO ---

    @Test
    void transicionARecibidoLimpiaLosCamposDePedidoEnCurso() {
        Proveedor existente = proveedor(5L, "Maderera Central");
        existente.setEstadoPedido(EstadoPedido.EN_PROGRESO);
        existente.setDetallePedidoActual("50 tablas de algarrobo");
        existente.setFechaPedido(LocalDate.of(2026, 6, 1));
        existente.setFechaLlegadaEstimada(LocalDate.of(2026, 7, 1));
        when(proveedorRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(proveedorRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrueAndIdNot(eq(EMPRESA_ID), any(), eq(5L)))
                .thenReturn(false);
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // El request trae valores para estos campos, pero como es una
        // transición nueva a RECIBIDO deben ignorarse: el auto-clear gana.
        ProveedorRequest request = request(EstadoPedido.RECIBIDO,
                "detalle que no debería quedar", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 25),
                "ultimo pedido que tampoco debería usarse");

        ProveedorDto resultado = proveedorService.actualizar(5L, request, principal);

        assertThat(resultado.estadoPedido()).isEqualTo(EstadoPedido.RECIBIDO);
        assertThat(resultado.ultimoPedidoDetalle()).isEqualTo("50 tablas de algarrobo");
        assertThat(resultado.detallePedidoActual()).isNull();
        assertThat(resultado.fechaPedido()).isNull();
        assertThat(resultado.fechaLlegadaEstimada()).isNull();
    }

    @Test
    void yaEstabaRecibidoYSeVuelveAGuardarComoRecibidoNoDisparaAutoClear() {
        Proveedor existente = proveedor(5L, "Maderera Central");
        existente.setEstadoPedido(EstadoPedido.RECIBIDO);
        existente.setUltimoPedidoDetalle("pedido anterior ya limpiado");
        when(proveedorRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(proveedorRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrueAndIdNot(eq(EMPRESA_ID), any(), eq(5L)))
                .thenReturn(false);
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProveedorRequest request = request(EstadoPedido.RECIBIDO,
                "detalle actual del request", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 25),
                "ultimo detalle editado a mano");

        ProveedorDto resultado = proveedorService.actualizar(5L, request, principal);

        assertThat(resultado.estadoPedido()).isEqualTo(EstadoPedido.RECIBIDO);
        assertThat(resultado.detallePedidoActual()).isEqualTo("detalle actual del request");
        assertThat(resultado.fechaPedido()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(resultado.fechaLlegadaEstimada()).isEqualTo(LocalDate.of(2026, 7, 25));
        assertThat(resultado.ultimoPedidoDetalle()).isEqualTo("ultimo detalle editado a mano");
    }

    @Test
    void transicionAUnEstadoDistintoDeRecibidoUsaLosValoresDelRequest() {
        Proveedor existente = proveedor(5L, "Maderera Central");
        existente.setEstadoPedido(EstadoPedido.SIN_PEDIDO);
        when(proveedorRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(proveedorRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrueAndIdNot(eq(EMPRESA_ID), any(), eq(5L)))
                .thenReturn(false);
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProveedorRequest request = request(EstadoPedido.EN_PROGRESO,
                "30 metros de cuero", LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 30), null);

        ProveedorDto resultado = proveedorService.actualizar(5L, request, principal);

        assertThat(resultado.estadoPedido()).isEqualTo(EstadoPedido.EN_PROGRESO);
        assertThat(resultado.detallePedidoActual()).isEqualTo("30 metros de cuero");
        assertThat(resultado.fechaPedido()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(resultado.fechaLlegadaEstimada()).isEqualTo(LocalDate.of(2026, 7, 30));
    }

    @Test
    void estadoPedidoNuloEnElRequestCaeASinPedido() {
        Proveedor existente = proveedor(5L, "Maderera Central");
        existente.setEstadoPedido(EstadoPedido.SIN_PEDIDO);
        when(proveedorRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(proveedorRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrueAndIdNot(eq(EMPRESA_ID), any(), eq(5L)))
                .thenReturn(false);
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProveedorRequest request = request(null, null, null, null, null);

        ProveedorDto resultado = proveedorService.actualizar(5L, request, principal);

        assertThat(resultado.estadoPedido()).isEqualTo(EstadoPedido.SIN_PEDIDO);
    }

    private ProveedorRequest nombreRequest(String nombre) {
        return new ProveedorRequest(nombre, null, null, EstadoPedido.SIN_PEDIDO, null, null, null, null);
    }

    private ProveedorRequest request(EstadoPedido estadoPedido, String detallePedidoActual, LocalDate fechaPedido,
                                      LocalDate fechaLlegadaEstimada, String ultimoPedidoDetalle) {
        return new ProveedorRequest("Maderera Central", null, null, estadoPedido,
                detallePedidoActual, fechaPedido, fechaLlegadaEstimada, ultimoPedidoDetalle);
    }

    private Proveedor proveedor(Long id, String nombre) {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(id);
        proveedor.setEmpresaId(EMPRESA_ID);
        proveedor.setNombre(nombre);
        proveedor.setActivo(true);
        return proveedor;
    }
}
