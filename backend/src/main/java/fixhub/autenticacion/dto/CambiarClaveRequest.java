package fixhub.autenticacion.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarClaveRequest(

    @NotBlank
    @Size(min = 8, max = 72)
    String claveActual,

    @NotBlank
    @Size(min = 8, max = 72)
    String claveNueva,

    @NotBlank
    @Size(min = 8, max = 72)
    String confirmacionClave

) {

    @AssertTrue(message = "La nueva clave y su confirmación no coinciden")
    public boolean isConfirmacionValida() {
        if (claveNueva == null || confirmacionClave == null) {
            return false;
        }

        return claveNueva.equals(confirmacionClave);
    }
}
