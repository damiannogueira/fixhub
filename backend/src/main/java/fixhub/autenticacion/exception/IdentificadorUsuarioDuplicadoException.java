package fixhub.autenticacion.exception;

public class IdentificadorUsuarioDuplicadoException
    extends RuntimeException {

    public IdentificadorUsuarioDuplicadoException(String identificador) {
        super(
            "Ya existe un usuario con el identificador: "
                + identificador
        );
    }
}
