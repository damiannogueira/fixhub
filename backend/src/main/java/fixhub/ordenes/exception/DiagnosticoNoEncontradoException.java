package fixhub.ordenes.exception;

public class DiagnosticoNoEncontradoException
    extends RuntimeException {

    public DiagnosticoNoEncontradoException(Long ordenId) {
        super(
            "No se encontró un diagnóstico para la orden "
                + ordenId
        );
    }
}
