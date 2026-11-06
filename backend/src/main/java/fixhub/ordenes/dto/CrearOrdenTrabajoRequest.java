package fixhub.ordenes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CrearOrdenTrabajoRequest(
    @NotNull @Positive Long equipoId,
    @Positive Long tecnicoId,
    @NotBlank String problemaInicial
) {
}
