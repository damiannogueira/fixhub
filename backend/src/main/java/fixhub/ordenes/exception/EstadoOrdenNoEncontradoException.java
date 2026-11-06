package fixhub.ordenes.exception;

import fixhub.ordenes.entity.NombreEstadoOrden;

public class EstadoOrdenNoEncontradoException extends RuntimeException {

    public EstadoOrdenNoEncontradoException(NombreEstadoOrden estado) {
        super("No se encontró el estado fijo de orden " + estado);
    }
}
