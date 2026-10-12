package fixhub.inventario.exception;

public class CodigoRepuestoDuplicadoException extends RuntimeException {

    public CodigoRepuestoDuplicadoException(String codigo) {
        super("Ya existe un repuesto con el código " + codigo);
    }
}