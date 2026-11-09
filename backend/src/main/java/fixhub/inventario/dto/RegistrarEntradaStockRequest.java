package fixhub.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegistrarEntradaStockRequest(

    @NotNull
    @Positive
    Integer cantidad,

    @NotBlank
    @Size(max = 255)
    String motivo,

    @Size(max = 120)
    String referencia

) {
}
