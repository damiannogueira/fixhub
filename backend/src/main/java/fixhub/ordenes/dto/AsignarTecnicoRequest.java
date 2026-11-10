package fixhub.ordenes.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * El motivo del cambio de técnico es opcional.
 */
public record AsignarTecnicoRequest(
    @NotNull @Positive Long tecnicoId,
    @Size(max = 500) String motivo
) {
}
