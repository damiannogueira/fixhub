package fixhub.ordenes.dto;

import java.time.LocalDateTime;

public record DiagnosticoResponse(
    Long id,
    Long ordenId,
    Long usuarioId,
    String descripcionFalla,
    String tareasRequeridas,
    LocalDateTime fecha
) {
}
