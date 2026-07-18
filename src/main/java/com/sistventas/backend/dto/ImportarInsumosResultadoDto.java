package com.sistventas.backend.dto;

import java.util.List;

// Resultado de una carga masiva de insumos vía Excel. El import es
// best-effort: una fila inválida no aborta el resto, así que "creados" puede
// ser menor a la cantidad de filas del archivo y "errores" trae el detalle
// legible (1 mensaje por fila fallida) para que el usuario corrija y reintente.
public record ImportarInsumosResultadoDto(
        int creados,
        List<String> errores
) {}
