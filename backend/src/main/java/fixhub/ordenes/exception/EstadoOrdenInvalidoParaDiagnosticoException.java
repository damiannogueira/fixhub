package fixhub.ordenes.exception;

import fixhub.ordenes.entity.NombreEstadoOrden;

public class EstadoOrdenInvalidoParaDiagnosticoException
    extends RuntimeException {

    public EstadoOrdenInvalidoParaDiagnosticoException(
        NombreEstadoOrden estado
    ) {
        super(
            "No se puede gestionar el diagnóstico cuando la orden "
                + "se encuentra en estado " + estado
        );
    }
}
