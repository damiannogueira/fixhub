package fixhub.ordenes.exception;

import fixhub.ordenes.entity.NombreEstadoOrden;

public class EstadoOrdenInvalidoParaRegistroTecnicoException
    extends RuntimeException {

    public EstadoOrdenInvalidoParaRegistroTecnicoException(
        NombreEstadoOrden estado
    ) {
        super(
            "No se puede registrar una tarea técnica cuando "
                + "la orden se encuentra en estado " + estado
        );
    }
}
