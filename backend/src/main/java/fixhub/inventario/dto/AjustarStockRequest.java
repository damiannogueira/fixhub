package fixhub.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AjustarStockRequest(

    @NotNull
    @PositiveOrZero
    Integer nuevoStock,

    @NotBlank
    @Size(max = 255)
    String motivo,

    @Size(max = 120)
    String referencia

) {
}
