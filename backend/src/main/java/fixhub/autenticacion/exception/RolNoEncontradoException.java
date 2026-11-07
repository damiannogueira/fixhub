package fixhub.autenticacion.exception;

import fixhub.autenticacion.entity.NombreRol;

public class RolNoEncontradoException extends RuntimeException {

    public RolNoEncontradoException(NombreRol nombreRol) {
        super("No se encontró el rol: " + nombreRol);
    }
}
