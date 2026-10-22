package fixhub.autenticacion.dto;

import fixhub.autenticacion.entity.NombreRol;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CrearUsuarioRequest(

    @Email
    @Size(max = 254)
    String email,

    @Size(max = 30)
    String telefono,

    @NotBlank
    @Size(min = 8, max = 72)
    String claveTemporal,

    @NotEmpty
    Set<NombreRol> roles

) {

    @AssertTrue(message = "Debe informarse al menos un email o teléfono")
    public boolean isMedioContactoValido() {
        boolean emailValido =
            email == null || !email.isBlank();

        boolean telefonoValido =
            telefono == null || !telefono.isBlank();

        boolean hayMedioContacto =
            (email != null && !email.isBlank())
                || (telefono != null && !telefono.isBlank());

        return emailValido
            && telefonoValido
            && hayMedioContacto;
    }
}
