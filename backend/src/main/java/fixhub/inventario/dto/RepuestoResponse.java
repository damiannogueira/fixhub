package fixhub.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RepuestoResponse(
    Long id,
    String codigo,
    String nombre,
    String descripcion,
    Integer stockActual,
    Integer stockMinimo,
    BigDecimal precioCosto,
    BigDecimal precioVenta,
    Boolean activo,
    LocalDateTime fechaCreacion,
    LocalDateTime fechaModificacion
) {
}
