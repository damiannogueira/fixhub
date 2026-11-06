package fixhub.ordenes.dto;

import jakarta.validation.constraints.NotBlank;

public record ActualizarDiagnosticoRequest(

    @NotBlank
    String descripcionFalla,

    @NotBlank
    String tareasRequeridas

) {
}
