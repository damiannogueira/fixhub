package fixhub.autenticacion.dto;

import fixhub.autenticacion.entity.NombreRol;

import java.util.Set;

public record LoginResponse(
    Long usuarioId,
    String identificador,
    String token,
    String tipo,
    Long expiraEnSegundos,
    Boolean requiereCambioClave,
    Set<NombreRol> roles
) {
}
