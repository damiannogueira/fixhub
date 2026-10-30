package fixhub.ordenes.dto;

import fixhub.ordenes.entity.NombreEstadoOrden;

import java.time.LocalDateTime;

public record OrdenClienteResponse(
    Long id,
    EquipoOrdenResponse equipo,
    NombreEstadoOrden estado,
    String problemaInicial,
    String motivoCancelacion,
    LocalDateTime fechaIngreso,
    LocalDateTime fechaEntrega
) {
}
