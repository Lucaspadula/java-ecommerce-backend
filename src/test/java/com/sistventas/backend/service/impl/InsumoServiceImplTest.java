package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ImportarInsumosResultadoDto;
import com.sistventas.backend.dto.InsumoDto;
import com.sistventas.backend.dto.InsumoRequest;
import com.sistventas.backend.entity.Insumo;
import com.sistventas.backend.entity.RolEmpresa;
import com.sistventas.backend.exception.AccionNoPermitidaException;
import com.sistventas.backend.repository.InsumoRepository;
import com.sistventas.backend.security.UserPrincipal;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
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

// Mismo patrón que ClienteServiceImplTest: nombre único case-insensitive por
// empresa, restaurar() sobre un insumo inactivo y listarInactivos(). El
// import/export de Excel queda fuera de este archivo (no es la lógica
// agregada en esta sesión).
@ExtendWith(MockitoExtension.class)
class InsumoServiceImplTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private InsumoRepository insumoRepository;

    @InjectMocks
    private InsumoServiceImpl insumoService;

    private final UserPrincipal principal = new UserPrincipal(99L, EMPRESA_ID, false, RolEmpresa.ADMIN);

    @Test
    void crearConNombreYaExistenteActivoLanzaExcepcion() {
        InsumoRequest request = request("Madera", "100", "10");
        when(insumoRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrue(EMPRESA_ID, "Madera"))
                .thenReturn(true);

        assertThatThrownBy(() -> insumoService.crear(request, principal))
                .isInstanceOf(AccionNoPermitidaException.class);

        verify(insumoRepository, never()).save(any());
    }

    @Test
    void crearConNombreLibrePersiste() {
        InsumoRequest request = request("Virola", "50", "20");
        when(insumoRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrue(EMPRESA_ID, "Virola"))
                .thenReturn(false);
        when(insumoRepository.save(any(Insumo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InsumoDto resultado = insumoService.crear(request, principal);

        assertThat(resultado.nombre()).isEqualTo("Virola");
        verify(insumoRepository).save(any(Insumo.class));
    }

    @Test
    void actualizarConNombreDeOtroInsumoLanzaExcepcion() {
        Insumo existente = insumo(5L, "Bombilla");
        when(insumoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(insumoRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrueAndIdNot(EMPRESA_ID, "Termo", 5L))
                .thenReturn(true);

        InsumoRequest request = request("Termo", "100", "5");

        assertThatThrownBy(() -> insumoService.actualizar(5L, request, principal))
                .isInstanceOf(AccionNoPermitidaException.class);

        verify(insumoRepository, never()).save(any());
    }

    @Test
    void actualizarConSuPropioNombreNoChocaContraSiMismo() {
        Insumo existente = insumo(5L, "Bombilla");
        when(insumoRepository.findByIdAndEmpresaId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
        when(insumoRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrueAndIdNot(EMPRESA_ID, "Bombilla", 5L))
                .thenReturn(false);
        when(insumoRepository.save(any(Insumo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InsumoRequest request = request("Bombilla", "100", "5");

        InsumoDto resultado = insumoService.actualizar(5L, request, principal);

        assertThat(resultado.nombre()).isEqualTo("Bombilla");
        verify(insumoRepository).existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrueAndIdNot(EMPRESA_ID, "Bombilla", 5L);
        verify(insumoRepository, never())
                .existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrue(anyLong(), eq("Bombilla"));
    }

    @Test
    void restaurarEncuentraInsumoInactivoLoActivaYLoGuarda() {
        Insumo inactivo = insumo(7L, "Cuero");
        inactivo.setActivo(false);
        when(insumoRepository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.of(inactivo));
        when(insumoRepository.save(any(Insumo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InsumoDto resultado = insumoService.restaurar(7L, principal);

        assertThat(inactivo.isActivo()).isTrue();
        assertThat(resultado.nombre()).isEqualTo("Cuero");
        verify(insumoRepository).save(inactivo);
    }

    @Test
    void listarInactivosDelegaEnElRepositoryYMapeaADto() {
        Insumo inactivo = insumo(8L, "Hilo");
        inactivo.setActivo(false);
        when(insumoRepository.findByEmpresaIdAndActivoFalse(EMPRESA_ID)).thenReturn(List.of(inactivo));

        List<InsumoDto> resultado = insumoService.listarInactivos(principal);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Hilo");
    }

    // --- importarDesdeExcel: best-effort fila-por-fila ---

    @Test
    void importarDesdeExcelConFilaValidaCreaElInsumo() {
        MockMultipartFile archivo = archivoConFilas(
                new Object[]{"Madera", 100, 50, 10, "unidad", "MATERIA_PRIMA"});
        when(insumoRepository.save(any(Insumo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportarInsumosResultadoDto resultado = insumoService.importarDesdeExcel(archivo, principal);

        assertThat(resultado.creados()).isEqualTo(1);
        assertThat(resultado.errores()).isEmpty();
        verify(insumoRepository).save(any(Insumo.class));
    }

    @Test
    void importarDesdeExcelConNombreDuplicadoReportaErrorDeEsaFilaYSigueConLasDemas() {
        // Best-effort: el nombre duplicado de la fila 1 no debe impedir que la
        // fila 2 (válida) se cree igual.
        MockMultipartFile archivo = archivoConFilas(
                new Object[]{"Duplicado", 100, 10, null, null, null},
                new Object[]{"Nuevo", 50, 5, null, null, null});
        when(insumoRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrue(EMPRESA_ID, "Duplicado"))
                .thenReturn(true);
        when(insumoRepository.save(any(Insumo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportarInsumosResultadoDto resultado = insumoService.importarDesdeExcel(archivo, principal);

        assertThat(resultado.creados()).isEqualTo(1);
        assertThat(resultado.errores()).hasSize(1);
        assertThat(resultado.errores().get(0))
                .contains("Fila 2")
                .contains("Ya existe un insumo activo con ese nombre");
    }

    @Test
    void importarDesdeExcelConCostoNoNumericoReportaErrorDeFilaSinRomperElImportCompleto() {
        MockMultipartFile archivo = archivoConFilas(
                new Object[]{"Vidrio", "abc", 10, null, null, null});

        ImportarInsumosResultadoDto resultado = insumoService.importarDesdeExcel(archivo, principal);

        assertThat(resultado.creados()).isZero();
        assertThat(resultado.errores()).hasSize(1);
        assertThat(resultado.errores().get(0))
                .contains("Fila 2")
                .contains("el costo debe ser un número");
        verify(insumoRepository, never()).save(any());
    }

    @Test
    void importarDesdeExcelConRolVacioEsValido() {
        MockMultipartFile archivo = archivoConFilas(
                new Object[]{"Hilo", 20, 5, null, null, null});
        when(insumoRepository.save(any(Insumo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportarInsumosResultadoDto resultado = insumoService.importarDesdeExcel(archivo, principal);

        assertThat(resultado.creados()).isEqualTo(1);
        assertThat(resultado.errores()).isEmpty();

        ArgumentCaptor<Insumo> captor = ArgumentCaptor.forClass(Insumo.class);
        verify(insumoRepository).save(captor.capture());
        assertThat(captor.getValue().getRol()).isNull();
    }

    @Test
    void importarDesdeExcelConRolInvalidoReportaErrorDeFilaConMensajeClaro() {
        MockMultipartFile archivo = archivoConFilas(
                new Object[]{"Cuero", 30, 8, null, null, "INVALIDO"});

        ImportarInsumosResultadoDto resultado = insumoService.importarDesdeExcel(archivo, principal);

        assertThat(resultado.creados()).isZero();
        assertThat(resultado.errores()).hasSize(1);
        assertThat(resultado.errores().get(0))
                .contains("Fila 2")
                .contains("el rol debe ser MATERIA_PRIMA, EMBALAJE o estar vacío");
        verify(insumoRepository, never()).save(any());
    }

    @Test
    void generarPlantillaNoExplotaYDevuelveBytesNoVacios() {
        byte[] resultado = insumoService.generarPlantilla();

        assertThat(resultado).isNotEmpty();
    }

    private InsumoRequest request(String nombre, String costo, String stock) {
        return new InsumoRequest(nombre, new BigDecimal(costo), new BigDecimal(stock), null, null, null);
    }

    private Insumo insumo(Long id, String nombre) {
        Insumo insumo = new Insumo();
        insumo.setId(id);
        insumo.setEmpresaId(EMPRESA_ID);
        insumo.setNombre(nombre);
        insumo.setCostoUnitario(BigDecimal.TEN);
        insumo.setStock(BigDecimal.TEN);
        insumo.setActivo(true);
        return insumo;
    }

    // Arma un .xlsx en memoria con el mismo layout que espera parsearFila:
    // Nombre, Costo, Stock, Stock Mínimo, Unidad, Rol. Cada fila es un
    // Object[] posicional; null deja la celda sin crear (equivale a "vacía"
    // para celdaTexto/celdaNumerica). La fila 0 es el encabezado (ignorado:
    // el parseo real arranca en la fila 1).
    private MockMultipartFile archivoConFilas(Object[]... filas) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Insumos");
            sheet.createRow(0);

            int rowIndex = 1;
            for (Object[] fila : filas) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < fila.length; i++) {
                    Object valor = fila[i];
                    if (valor == null) {
                        continue;
                    }
                    if (valor instanceof Number numero) {
                        row.createCell(i).setCellValue(numero.doubleValue());
                    } else {
                        row.createCell(i).setCellValue(valor.toString());
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return new MockMultipartFile("file", "insumos.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    baos.toByteArray());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
