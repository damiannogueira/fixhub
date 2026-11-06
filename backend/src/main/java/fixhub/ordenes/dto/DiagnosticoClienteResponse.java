package fixhub.ordenes.dto;

import java.time.LocalDateTime;

public record DiagnosticoClienteResponse(
    Long id,
    Long ordenId,
    String descripcionFalla,
    String tareasRequeridas,
    LocalDateTime fecha
) {
}
