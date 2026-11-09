package fixhub.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AjusteStockRequest(
    @NotNull Integer cantidad,
    @NotBlank String motivo,
    String referencia
) {}
