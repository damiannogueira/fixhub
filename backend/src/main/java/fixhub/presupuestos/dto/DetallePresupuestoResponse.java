package fixhub.presupuestos.dto;

import java.math.BigDecimal;

public record DetallePresupuestoResponse(Long repuestoId, String repuesto, Integer cantidad, BigDecimal precioUnitario) {}
