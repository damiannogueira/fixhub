package fixhub.presupuestos.exception;

public class PresupuestoNoEncontradoException extends RuntimeException {

    public PresupuestoNoEncontradoException() {
        super("Presupuesto no encontrado.");
    }
}
