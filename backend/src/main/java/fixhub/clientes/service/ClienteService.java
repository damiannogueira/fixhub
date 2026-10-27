package fixhub.clientes.service;

import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.service.UsuarioService;
import fixhub.clientes.dto.ActualizarClienteRequest;
import fixhub.clientes.dto.ClienteResponse;
import fixhub.clientes.dto.CrearClienteConCuentaRequest;
import fixhub.clientes.dto.CrearClienteConUsuarioRequest;
import fixhub.clientes.entity.Cliente;
import fixhub.clientes.exception.ClienteNoEncontradoException;
import fixhub.clientes.exception.UsuarioYaVinculadoAClienteException;
import fixhub.clientes.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioService usuarioService;

    public ClienteService(
        ClienteRepository clienteRepository,
        UsuarioService usuarioService
    ) {
        this.clienteRepository = clienteRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public ClienteResponse crearConUsuario(
        CrearClienteConUsuarioRequest request
    ) {
        Usuario usuario = usuarioService.buscarEntidadPorId(
            request.usuarioId()
        );
        validarUsuarioDisponible(usuario.getId());
        usuarioService.asegurarRolCliente(usuario);

        return guardarCliente(
            usuario,
            request.nombre(),
            request.apellido(),
            request.dni(),
            request.direccion()
        );
    }

    @Transactional
    public ClienteResponse crearConCuenta(
        CrearClienteConCuentaRequest request
    ) {
        // La cuenta y el cliente se crean en la misma transacción
        // para evitar cuentas sin vincular.
        Usuario usuario = usuarioService.crearCuentaCliente(
            request.email(),
            request.telefono(),
            request.claveTemporal()
        );

        return guardarCliente(
            usuario,
            request.nombre(),
            request.apellido(),
            request.dni(),
            request.direccion()
        );
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepository
            .findAllByOrderByApellidoAscNombreAscIdAsc()
            .stream()
            .map(this::convertirAResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return convertirAResponse(buscarEntidadPorId(id));
    }

    @Transactional
    public ClienteResponse actualizar(
        Long id,
        ActualizarClienteRequest request
    ) {
        Cliente cliente = buscarEntidadPorId(id);
        cliente.setNombre(request.nombre().trim());
        cliente.setApellido(request.apellido().trim());
        cliente.setDni(normalizarTextoOpcional(request.dni()));
        cliente.setDireccion(
            normalizarTextoOpcional(request.direccion())
        );

        return convertirAResponse(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPropio(Long usuarioId) {
        return convertirAResponse(buscarEntidadPorUsuarioId(usuarioId));
    }

    @Transactional(readOnly = true)
    public Cliente buscarEntidadPorId(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new ClienteNoEncontradoException(id));
    }

    @Transactional(readOnly = true)
    public Cliente buscarEntidadPorUsuarioId(Long usuarioId) {
        return clienteRepository.findByUsuarioId(usuarioId)
            .orElseThrow(
                () -> ClienteNoEncontradoException.paraUsuario(usuarioId)
            );
    }

    private void validarUsuarioDisponible(Long usuarioId) {
        if (clienteRepository.existsByUsuarioId(usuarioId)) {
            throw new UsuarioYaVinculadoAClienteException(usuarioId);
        }
    }

    private ClienteResponse guardarCliente(
        Usuario usuario,
        String nombre,
        String apellido,
        String dni,
        String direccion
    ) {
        Cliente cliente = new Cliente(
            usuario,
            nombre.trim(),
            apellido.trim(),
            normalizarTextoOpcional(dni),
            normalizarTextoOpcional(direccion)
        );

        return convertirAResponse(clienteRepository.save(cliente));
    }

    private ClienteResponse convertirAResponse(Cliente cliente) {
        Usuario usuario = cliente.getUsuario();

        return new ClienteResponse(
            cliente.getId(),
            cliente.getNombre(),
            cliente.getApellido(),
            cliente.getDni(),
            cliente.getDireccion(),
            usuario.getId(),
            usuario.getEmail(),
            usuario.getTelefono()
        );
    }

    private String normalizarTextoOpcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }
}
