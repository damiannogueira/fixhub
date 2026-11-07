package fixhub.autenticacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

    @NotBlank
    @Size(max = 254)
    String identificador,

    @NotBlank
    @Size(min = 8, max = 72)
    String clave

) {
}
