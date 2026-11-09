package fixhub.presupuestos.dto;

import fixhub.presupuestos.entity.EstadoPresupuesto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PresupuestoResponse(
    Long id, Long ordenId, EstadoPresupuesto estado, BigDecimal costoManoObra,
    BigDecimal total, LocalDateTime fechaCreacion, LocalDateTime fechaPublicacion,
    LocalDateTime fechaDecision, List<DetallePresupuestoResponse> detalles
) {}
