package fixhub.autenticacion.exception;

public class UsuarioNoEsTecnicoException extends RuntimeException {

    public UsuarioNoEsTecnicoException(Long usuarioId) {
        super("El usuario " + usuarioId + " no posee el rol Técnico");
    }
}
