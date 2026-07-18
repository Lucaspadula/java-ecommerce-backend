package com.sistventas.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResumenDto(
        BigDecimal totalVentas,
        BigDecimal gananciaTotal,
        BigDecimal margenPromedioPorcentaje,
        List<EstadoConteoDto> pedidosPorEstado,
        long clientesActivos,
        long productosActivos,
        long proveedoresConPedidoEnCurso,
        List<VentaResumenDto> proximasEntregas,
        List<ProductoSinStockDto> productosSinStock,
        List<InsumoStockBajoDto> insumosStockBajo,
        List<TopProductoDto> topProductos,
        List<GananciaMesDto> gananciaPorMes,
        List<UnidadesMesDto> unidadesPorMes
) {}
