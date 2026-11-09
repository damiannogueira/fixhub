package fixhub.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MovimientoStockRequest(
    @NotNull @Positive Integer cantidad,
    @NotBlank String motivo,
    String referencia
) {}
