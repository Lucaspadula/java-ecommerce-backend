package com.sistventas.backend.service.impl;

import com.sistventas.backend.dto.ImportarInsumosResultadoDto;
import com.sistventas.backend.dto.InsumoDto;
import com.sistventas.backend.dto.InsumoRequest;
import com.sistventas.backend.entity.Insumo;
import com.sistventas.backend.entity.RolInsumo;
import com.sistventas.backend.exception.AccionNoPermitidaException;
import com.sistventas.backend.exception.ArchivoInvalidoException;
import com.sistventas.backend.exception.InsumoNoEncontradoException;
import com.sistventas.backend.exception.SinEmpresaException;
import com.sistventas.backend.repository.InsumoRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.InsumoService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class InsumoServiceImpl implements InsumoService {

    // Mismo orden de columnas en la plantilla generada y en el parseo del
    // import: Nombre, Costo Unitario, Stock, Stock Mínimo, Unidad de Medida, Rol.
    private static final String[] ENCABEZADOS_PLANTILLA =
            {"Nombre", "Costo Unitario", "Stock", "Stock Mínimo", "Unidad de Medida", "Rol (MATERIA_PRIMA/EMBALAJE, opcional)"};

    private final InsumoRepository insumoRepository;

    public InsumoServiceImpl(InsumoRepository insumoRepository) {
        this.insumoRepository = insumoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsumoDto> listar(UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        return insumoRepository.findByEmpresaIdAndActivoTrue(empresaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsumoDto> listarInactivos(UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        return insumoRepository.findByEmpresaIdAndActivoFalse(empresaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InsumoDto obtener(Long id, UserPrincipal principal) {
        return toDto(buscarPorEmpresa(id, principal));
    }

    @Override
    @Transactional
    public InsumoDto crear(InsumoRequest request, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        validarNombreUnico(request.nombre(), null, empresaId);

        Insumo insumo = new Insumo();
        insumo.setEmpresaId(empresaId);
        insumo.setActivo(true);
        insumo.setFechaAlta(LocalDateTime.now());
        aplicarDatos(insumo, request);

        return toDto(insumoRepository.save(insumo));
    }

    @Override
    @Transactional
    public InsumoDto actualizar(Long id, InsumoRequest request, UserPrincipal principal) {
        Insumo insumo = buscarPorEmpresa(id, principal);
        validarNombreUnico(request.nombre(), id, insumo.getEmpresaId());
        aplicarDatos(insumo, request);
        return toDto(insumoRepository.save(insumo));
    }

    // Comparación case-insensitive contra insumos activos de la misma
    // empresa. idActual null en el alta; en la edición se excluye al propio
    // insumo para no chocar contra sí mismo.
    private void validarNombreUnico(String nombre, Long idActual, Long empresaId) {
        boolean duplicado = idActual == null
                ? insumoRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrue(empresaId, nombre)
                : insumoRepository.existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrueAndIdNot(empresaId, nombre, idActual);
        if (duplicado) {
            throw new AccionNoPermitidaException("Ya existe un insumo activo con ese nombre");
        }
    }

    @Override
    @Transactional
    public void eliminar(Long id, UserPrincipal principal) {
        Insumo insumo = buscarPorEmpresa(id, principal);
        insumo.setActivo(false);
        insumoRepository.save(insumo);
    }

    @Override
    @Transactional
    public InsumoDto restaurar(Long id, UserPrincipal principal) {
        Insumo insumo = buscarPorEmpresa(id, principal);
        insumo.setActivo(true);
        return toDto(insumoRepository.save(insumo));
    }

    @Override
    public byte[] generarPlantilla() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Insumos");

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row filaEncabezado = sheet.createRow(0);
            for (int i = 0; i < ENCABEZADOS_PLANTILLA.length; i++) {
                var celda = filaEncabezado.createCell(i);
                celda.setCellValue(ENCABEZADOS_PLANTILLA[i]);
                celda.setCellStyle(headerStyle);
            }

            // Fila de ejemplo para que quede claro el formato esperado de cada columna.
            Row filaEjemplo = sheet.createRow(1);
            filaEjemplo.createCell(0).setCellValue("Madera de algarrobo");
            filaEjemplo.createCell(1).setCellValue(3000);
            filaEjemplo.createCell(2).setCellValue(50);
            filaEjemplo.createCell(3).setCellValue(10);
            filaEjemplo.createCell(4).setCellValue("unidad");
            filaEjemplo.createCell(5).setCellValue("MATERIA_PRIMA");

            for (int i = 0; i < ENCABEZADOS_PLANTILLA.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo generar la plantilla de insumos", ex);
        }
    }

    @Override
    @Transactional
    public ImportarInsumosResultadoDto importarDesdeExcel(MultipartFile file, UserPrincipal principal) {
        if (file == null || file.isEmpty()) {
            throw new ArchivoInvalidoException("El archivo es obligatorio");
        }

        int creados = 0;
        List<String> errores = new ArrayList<>();

        // La apertura del workbook queda aislada del procesamiento de filas a
        // propósito: si algo falla acá es porque el archivo en sí es inválido
        // (no un .xlsx, corrupto, etc.). Si dejáramos que este catch envolviera
        // también el loop de abajo, una excepción de negocio real (ej.
        // SinEmpresaException al crear un insumo) quedaría enmascarada como si
        // el archivo fuera inválido, lo cual sería un mensaje engañoso.
        try (XSSFWorkbook workbook = abrirWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (filaVacia(row)) {
                    continue;
                }

                int filaVisible = rowIndex + 1;
                try {
                    InsumoRequest request = parsearFila(row);
                    crear(request, principal);
                    creados++;
                } catch (IllegalArgumentException | AccionNoPermitidaException ex) {
                    // La segunda también acá: un nombre duplicado (ver
                    // validarNombreUnico en crear()) es un error de esa fila
                    // puntual, no del archivo entero — mismo criterio
                    // best-effort que un dato mal tipeado.
                    errores.add("Fila " + filaVisible + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer el archivo de insumos", ex);
        }

        return new ImportarInsumosResultadoDto(creados, errores);
    }

    private XSSFWorkbook abrirWorkbook(MultipartFile file) {
        try {
            return new XSSFWorkbook(file.getInputStream());
        } catch (Exception ex) {
            throw new ArchivoInvalidoException("El archivo no es un Excel válido");
        }
    }

    private boolean filaVacia(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < ENCABEZADOS_PLANTILLA.length; i++) {
            if (!celdaTexto(row, i).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private InsumoRequest parsearFila(Row row) {
        String nombre = celdaTexto(row, 0);
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("el nombre es obligatorio");
        }

        BigDecimal costoUnitario = celdaNumerica(row, 1, "el costo debe ser un número");
        if (costoUnitario == null) {
            throw new IllegalArgumentException("el costo es obligatorio");
        }
        if (costoUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("el costo no puede ser negativo");
        }

        BigDecimal stock = celdaNumerica(row, 2, "el stock debe ser un número");
        if (stock == null) {
            throw new IllegalArgumentException("el stock es obligatorio");
        }
        if (stock.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("el stock no puede ser negativo");
        }

        // Stock mínimo y unidad de medida son opcionales, igual que en InsumoRequest.
        BigDecimal stockMinimo = celdaNumerica(row, 3, "el stock mínimo debe ser un número");

        String unidadMedida = celdaTexto(row, 4);
        unidadMedida = unidadMedida.isBlank() ? null : unidadMedida;

        RolInsumo rol = parsearRol(celdaTexto(row, 5));

        return new InsumoRequest(nombre, costoUnitario, stock, stockMinimo, unidadMedida, rol);
    }

    // Opcional: celda vacía es válido (insumo sin clasificar, como siempre).
    // Cualquier texto que no matchee exactamente un valor del enum es un
    // error de fila, no se adivina ni se ignora en silencio.
    private RolInsumo parsearRol(String texto) {
        if (texto.isBlank()) {
            return null;
        }
        try {
            return RolInsumo.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("el rol debe ser MATERIA_PRIMA, EMBALAJE o estar vacío");
        }
    }

    private String celdaTexto(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            default -> "";
        };
    }

    private BigDecimal celdaNumerica(Row row, int index, String mensajeError) {
        Cell cell = row.getCell(index);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.STRING) {
            String texto = cell.getStringCellValue().trim();
            if (texto.isEmpty()) {
                return null;
            }
            try {
                return new BigDecimal(texto.replace(",", "."));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(mensajeError);
            }
        }
        throw new IllegalArgumentException(mensajeError);
    }

    private void aplicarDatos(Insumo insumo, InsumoRequest request) {
        insumo.setNombre(request.nombre());
        insumo.setCostoUnitario(request.costoUnitario());
        insumo.setStock(request.stock());
        insumo.setStockMinimo(request.stockMinimo());
        insumo.setUnidadMedida(request.unidadMedida());
        insumo.setRol(request.rol());
    }

    private Insumo buscarPorEmpresa(Long id, UserPrincipal principal) {
        Long empresaId = empresaIdOrThrow(principal);
        return insumoRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(InsumoNoEncontradoException::new);
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

    private InsumoDto toDto(Insumo insumo) {
        return new InsumoDto(
                insumo.getId(),
                insumo.getNombre(),
                insumo.getCostoUnitario(),
                insumo.getStock(),
                insumo.getStockMinimo(),
                insumo.getUnidadMedida(),
                insumo.isActivo(),
                insumo.getFechaAlta(),
                insumo.getRol()
        );
    }
}
