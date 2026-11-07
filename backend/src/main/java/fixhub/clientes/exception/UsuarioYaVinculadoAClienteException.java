package fixhub.clientes.exception;

public class UsuarioYaVinculadoAClienteException extends RuntimeException {

    public UsuarioYaVinculadoAClienteException(Long usuarioId) {
        super("El usuario " + usuarioId + " ya está vinculado a un cliente");
    }
}
