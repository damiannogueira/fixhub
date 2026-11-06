package fixhub.ordenes.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearRegistroTecnicoRequest(

    @NotBlank
    String tareaRealizada,

    String observacion

) {
}
