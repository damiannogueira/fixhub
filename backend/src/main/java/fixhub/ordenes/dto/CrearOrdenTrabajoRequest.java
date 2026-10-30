package fixhub.ordenes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearOrdenTrabajoRequest(
    @NotNull Long equipoId,
    Long tecnicoId,
    @NotBlank String problemaInicial
) {
}
