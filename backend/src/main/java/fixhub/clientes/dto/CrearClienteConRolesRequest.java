package fixhub.clientes.dto;

import fixhub.autenticacion.entity.NombreRol;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CrearClienteConRolesRequest(
    @Email @Size(max = 254) String email,
    @Size(max = 30) String telefono,
    @NotBlank @Size(min = 8, max = 72) String claveTemporal,
    @NotEmpty Set<NombreRol> roles,
    @NotBlank @Size(max = 255) String nombre,
    @NotBlank @Size(max = 255) String apellido,
    @Size(max = 255) String dni,
    @Size(max = 255) String direccion
) {

    @AssertTrue(message = "Debe informarse al menos un email o teléfono")
    public boolean isMedioContactoValido() {
        return (email != null && !email.isBlank())
            || (telefono != null && !telefono.isBlank());
    }

    @AssertTrue(message = "El usuario debe tener el rol Cliente")
    public boolean isSoloRolCliente() {
        return roles != null
            && roles.size() == 1
            && roles.contains(NombreRol.CLIENTE);
    }
}
