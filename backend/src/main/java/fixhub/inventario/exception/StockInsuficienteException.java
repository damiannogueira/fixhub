package fixhub.inventario.exception;

public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(
        Long repuestoId,
        Integer disponible,
        Integer solicitado
    ) {
        super(
            "Stock insuficiente para el repuesto "
                + repuestoId
                + ". Disponible: "
                + disponible
                + ", solicitado: "
                + solicitado
        );
    }
}
