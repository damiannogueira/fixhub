package fixhub.ordenes.dto;

import fixhub.ordenes.entity.NombreEstadoOrden;

import java.time.LocalDateTime;

public record OrdenTrabajoResponse(
    Long id,
    EquipoOrdenResponse equipo,
    ClienteOrdenResponse cliente,
    Long tecnicoId,
    NombreEstadoOrden estado,
    String problemaInicial,
    String motivoCancelacion,
    LocalDateTime fechaIngreso,
    LocalDateTime fechaEntrega
) {
}
