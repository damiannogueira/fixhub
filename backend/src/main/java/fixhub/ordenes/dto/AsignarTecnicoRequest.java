package fixhub.ordenes.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AsignarTecnicoRequest(@NotNull @Positive Long tecnicoId) {
}
