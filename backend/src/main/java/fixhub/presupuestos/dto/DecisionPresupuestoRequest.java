package fixhub.presupuestos.dto;

import fixhub.presupuestos.entity.EstadoPresupuesto;
import jakarta.validation.constraints.NotNull;

public record DecisionPresupuestoRequest(@NotNull EstadoPresupuesto decision) {}
