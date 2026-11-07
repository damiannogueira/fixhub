package fixhub.ordenes.dto;

public record EquipoOrdenResponse(
    Long id,
    String tipo,
    String marca,
    String modelo,
    String numeroSerie,
    String observaciones
) {
}
