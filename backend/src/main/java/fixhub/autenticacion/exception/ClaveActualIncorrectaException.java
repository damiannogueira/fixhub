package fixhub.autenticacion.exception;

public class ClaveActualIncorrectaException
    extends RuntimeException {

    public ClaveActualIncorrectaException() {
        super("La clave actual es incorrecta");
    }
}
