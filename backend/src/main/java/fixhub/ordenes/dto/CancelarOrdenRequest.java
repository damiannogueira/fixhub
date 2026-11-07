package fixhub.ordenes.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelarOrdenRequest(@NotBlank String motivo) {
}
