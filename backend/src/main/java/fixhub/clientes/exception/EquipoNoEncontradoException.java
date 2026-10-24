package fixhub.clientes.exception;

public class EquipoNoEncontradoException extends RuntimeException {

    public EquipoNoEncontradoException(Long id) {
        super("No se encontró el equipo con id " + id);
    }
}
