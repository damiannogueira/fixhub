package fixhub.inventario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ActualizarRepuestoRequest(

    @NotBlank
    @Size(max = 50)
    String codigo,

    @NotBlank
    @Size(max = 120)
    String nombre,

    String descripcion,

    @NotNull
    @PositiveOrZero
    Integer stockMinimo,

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    BigDecimal precioCosto,

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    BigDecimal precioVenta

) {
}
