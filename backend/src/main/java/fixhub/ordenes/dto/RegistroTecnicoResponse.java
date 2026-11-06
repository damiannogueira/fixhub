package fixhub.ordenes.dto;

import java.time.LocalDateTime;

public record RegistroTecnicoResponse(
    Long id,
    Long ordenId,
    Long usuarioId,
    String tareaRealizada,
    String observacion,
    LocalDateTime fecha
) {
}
