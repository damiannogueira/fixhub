package fixhub.ordenes.exception;

import fixhub.ordenes.entity.NombreEstadoOrden;

public class TransicionEstadoInvalidaException extends RuntimeException {

    public TransicionEstadoInvalidaException(
        NombreEstadoOrden origen,
        NombreEstadoOrden destino
    ) {
        super("No se permite cambiar la orden de " + origen + " a " + destino);
    }
}
