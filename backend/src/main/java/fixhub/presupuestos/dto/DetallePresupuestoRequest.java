package fixhub.presupuestos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DetallePresupuestoRequest(@NotNull @Positive Long repuestoId, @NotNull @Positive Integer cantidad) {}
