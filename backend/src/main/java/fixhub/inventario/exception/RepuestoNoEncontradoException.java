package fixhub.inventario.exception;

public class RepuestoNoEncontradoException extends RuntimeException {

    public RepuestoNoEncontradoException(Long id) {
        super("No se encontró un repuesto activo con id " + id);
    }
}