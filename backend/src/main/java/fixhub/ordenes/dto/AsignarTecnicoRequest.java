package fixhub.ordenes.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * El motivo es obligatorio solo al cambiar un técnico ya asignado
 * (lo valida el servicio); en la primera asignación puede omitirse.
 */
public record AsignarTecnicoRequest(
    @NotNull @Positive Long tecnicoId,
    @Size(max = 500) String motivo
) {
}
