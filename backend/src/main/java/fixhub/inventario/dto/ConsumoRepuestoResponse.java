package fixhub.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsumoRepuestoResponse(
    Long id,
    Long ordenId,
    Long repuestoId,
    String codigoRepuesto,
    String nombreRepuesto,
    Long usuarioId,
    Integer cantidad,
    BigDecimal precioUnitario,
    LocalDateTime fecha
) {
}
