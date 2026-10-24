package fixhub.clientes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarEquipoRequest(
    @NotBlank @Size(max = 255) String tipo,
    @Size(max = 255) String marca,
    @Size(max = 255) String modelo,
    @Size(max = 255) String numeroSerie,
    String observaciones
) {
}
