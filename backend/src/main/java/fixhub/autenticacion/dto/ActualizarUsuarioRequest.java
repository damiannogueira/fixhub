package fixhub.autenticacion.dto;

import fixhub.autenticacion.entity.NombreRol;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record ActualizarUsuarioRequest(
    @Email
    @Size(max = 254)
    String email,

    @Size(max = 30)
    String telefono,

    @Size(min = 8, max = 72)
    String claveTemporal,

    @NotEmpty Set<NombreRol> roles,

    Boolean activo
) {

    @AssertTrue(message = "Debe informarse al menos un email o teléfono")
    public boolean isMedioContactoValido() {
        return (email != null && !email.isBlank())
            || (telefono != null && !telefono.isBlank());
    }

}
