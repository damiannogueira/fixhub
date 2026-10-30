package fixhub.ordenes.dto;

import fixhub.ordenes.entity.NombreEstadoOrden;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoOrdenRequest(
    @NotNull NombreEstadoOrden estado
) {
}
