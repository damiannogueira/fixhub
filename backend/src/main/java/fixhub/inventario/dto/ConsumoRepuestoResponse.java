package fixhub.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsumoRepuestoResponse(
    Long id,
    Long ordenId,
    Long repuestoId,
    String repuesto,
    Integer cantidad,
    BigDecimal precioUnitario,
    Long usuarioId,
    String usuarioNombre,
    LocalDateTime fecha
) {}
