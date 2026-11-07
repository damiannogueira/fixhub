package fixhub.clientes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarClienteRequest(
    @NotBlank @Size(max = 255) String nombre,
    @NotBlank @Size(max = 255) String apellido,
    @Size(max = 255) String dni,
    @Size(max = 255) String direccion
) {
}
