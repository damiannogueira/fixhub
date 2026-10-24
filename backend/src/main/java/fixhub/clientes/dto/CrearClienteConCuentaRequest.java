package fixhub.clientes.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearClienteConCuentaRequest(
    @Email @Size(max = 254) String email,
    @Size(max = 30) String telefono,
    @NotBlank @Size(min = 8, max = 72) String claveTemporal,
    @NotBlank @Size(max = 255) String nombre,
    @NotBlank @Size(max = 255) String apellido,
    @Size(max = 255) String dni,
    @Size(max = 255) String direccion
) {

    @AssertTrue(message = "Debe informarse al menos un email o teléfono")
    public boolean isMedioContactoValido() {
        boolean emailValido = email == null || !email.isBlank();
        boolean telefonoValido = telefono == null || !telefono.isBlank();
        boolean hayContacto =
            (email != null && !email.isBlank())
                || (telefono != null && !telefono.isBlank());

        return emailValido && telefonoValido && hayContacto;
    }
}
