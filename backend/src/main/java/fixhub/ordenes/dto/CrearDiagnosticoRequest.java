package fixhub.ordenes.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearDiagnosticoRequest(

    @NotBlank
    String descripcionFalla,

    @NotBlank
    String tareasRequeridas

) {
}
