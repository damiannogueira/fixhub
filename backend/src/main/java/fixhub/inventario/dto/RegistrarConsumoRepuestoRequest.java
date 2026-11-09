package fixhub.inventario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegistrarConsumoRepuestoRequest(

    @NotNull
    @Positive
    Long repuestoId,

    @NotNull
    @Positive
    Integer cantidad

) {
}
