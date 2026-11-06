package fixhub.ordenes.exception;

public class DiagnosticoYaRegistradoException
    extends RuntimeException {

    public DiagnosticoYaRegistradoException(Long ordenId) {
        super(
            "La orden " + ordenId
                + " ya posee un diagnóstico registrado"
        );
    }
}
