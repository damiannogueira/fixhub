package fixhub.ordenes.dto;

import java.time.LocalDateTime;

/**
 * Vista de un registro técnico para el Cliente: solo la tarea realizada y su
 * fecha. No incluye la observación interna ni datos del técnico.
 */
public record RegistroTecnicoClienteResponse(
    Long id,
    Long ordenId,
    String tareaRealizada,
    LocalDateTime fecha
) {
}
