package fixhub.autenticacion.dto;

import fixhub.autenticacion.entity.NombreRol;

import java.time.LocalDateTime;
import java.util.Set;

public record UsuarioResponse(
    Long id,
    String email,
    String telefono,
    Boolean activo,
    Boolean requiereCambioClave,
    LocalDateTime fechaCreacion,
    Set<NombreRol> roles
) {
}
