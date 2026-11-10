package fixhub.ordenes.dto;

import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.entity.TipoEventoOrden;

import java.time.LocalDateTime;

public record HistorialOrdenResponse(
    Long id,
    TipoEventoOrden tipoEvento,
    NombreEstadoOrden estadoAnterior,
    NombreEstadoOrden estadoNuevo,
    String comentario,
    Long usuarioId,
    String usuarioNombre,
    boolean visibleCliente,
    LocalDateTime fecha
) {
}
