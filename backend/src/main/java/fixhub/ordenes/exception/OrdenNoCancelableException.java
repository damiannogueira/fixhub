package fixhub.ordenes.exception;

import fixhub.ordenes.entity.NombreEstadoOrden;

public class OrdenNoCancelableException extends RuntimeException {

    public OrdenNoCancelableException(NombreEstadoOrden estado) {
        super("No se puede cancelar una orden en estado " + estado);
    }
}
