package fixhub.ordenes.exception;

public class OrdenTrabajoNoEncontradaException extends RuntimeException {

    public OrdenTrabajoNoEncontradaException(Long id) {
        super("No se encontró la orden de trabajo con id " + id);
    }
}
