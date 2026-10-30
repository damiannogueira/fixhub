package fixhub.ordenes.exception;

public class TecnicoRequeridoException extends RuntimeException {

    public TecnicoRequeridoException() {
        super("La orden debe tener un técnico asignado antes del diagnóstico");
    }
}
