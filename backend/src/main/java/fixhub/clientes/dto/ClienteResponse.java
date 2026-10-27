package fixhub.clientes.dto;

public record ClienteResponse(
    Long id,
    String nombre,
    String apellido,
    String dni,
    String direccion,
    Long usuarioId,
    String email,
    String telefono
) {
}
