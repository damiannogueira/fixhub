package fixhub.inventario.exception;

import fixhub.ordenes.entity.NombreEstadoOrden;

public class EstadoOrdenInvalidoParaConsumoException
    extends RuntimeException {

    public EstadoOrdenInvalidoParaConsumoException(
        NombreEstadoOrden estado
    ) {
        super(
            "No se pueden registrar consumos con la orden "
                + "en estado "
                + estado
        );
    }
}
