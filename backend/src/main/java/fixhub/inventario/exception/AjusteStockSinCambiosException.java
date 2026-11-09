package fixhub.inventario.exception;

public class AjusteStockSinCambiosException
    extends RuntimeException {

    public AjusteStockSinCambiosException(Long repuestoId) {
        super(
            "El stock indicado coincide con el stock actual "
                + "del repuesto "
                + repuestoId
        );
    }
}
