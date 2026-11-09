package fixhub.inventario.dto;

import fixhub.inventario.entity.TipoMovimientoStock;

import java.time.LocalDateTime;

public record MovimientoStockResponse(
    Long id,
    Long repuestoId,
    String codigoRepuesto,
    String nombreRepuesto,
    Long usuarioId,
    Long consumoId,
    TipoMovimientoStock tipo,
    Integer cantidad,
    String motivo,
    String referencia,
    LocalDateTime fecha
) {
}
