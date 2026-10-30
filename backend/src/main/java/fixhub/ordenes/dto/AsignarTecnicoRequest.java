package fixhub.ordenes.dto;

import jakarta.validation.constraints.NotNull;

public record AsignarTecnicoRequest(@NotNull Long tecnicoId) {
}
