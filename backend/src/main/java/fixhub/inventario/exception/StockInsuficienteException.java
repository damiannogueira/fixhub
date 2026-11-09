package fixhub.inventario.exception;

public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(String repuesto, int disponible, int solicitado) {
        super("Stock insuficiente para " + repuesto + ": disponible " + disponible + ", solicitado " + solicitado + ".");
    }
}
