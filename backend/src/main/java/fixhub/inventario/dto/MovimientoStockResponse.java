package fixhub.inventario.dto;

import fixhub.inventario.entity.TipoMovimientoStock;
import java.time.LocalDateTime;

public record MovimientoStockResponse(
    Long id,
    Long repuestoId,
    TipoMovimientoStock tipo,
    Integer cantidad,
    String motivo,
    String referencia,
    Long usuarioId,
    String usuarioNombre,
    Long consumoId,
    LocalDateTime fecha
) {}
