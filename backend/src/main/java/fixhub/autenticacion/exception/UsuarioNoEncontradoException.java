package fixhub.autenticacion.exception;

public class UsuarioNoEncontradoException
    extends RuntimeException {

    public UsuarioNoEncontradoException(Long usuarioId) {
        super(
            "No se encontró el usuario con ID: "
                + usuarioId
        );
    }
}
