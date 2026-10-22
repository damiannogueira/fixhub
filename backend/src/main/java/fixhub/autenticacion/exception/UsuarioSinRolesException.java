package fixhub.autenticacion.exception;

public class UsuarioSinRolesException extends RuntimeException {

    public UsuarioSinRolesException(Long usuarioId) {
        super(
            "No se puede quitar el último rol del usuario con id "
                + usuarioId
        );
    }
}
