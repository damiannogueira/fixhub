package fixhub.presupuestos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record CrearPresupuestoRequest(
    @NotNull @DecimalMin("0.0") BigDecimal costoManoObra,
    @Valid List<DetallePresupuestoRequest> detalles
) {}
