package fixhub.clientes.exception;

public class ClienteNoEncontradoException extends RuntimeException {

    public ClienteNoEncontradoException(Long id) {
        super("No se encontró el cliente con id " + id);
    }

    public static ClienteNoEncontradoException paraUsuario(Long usuarioId) {
        return new ClienteNoEncontradoException(
            "No se encontró un cliente vinculado al usuario " + usuarioId
        );
    }

    private ClienteNoEncontradoException(String message) {
        super(message);
    }
}
