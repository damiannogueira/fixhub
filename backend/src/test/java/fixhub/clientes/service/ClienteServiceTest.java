package fixhub.clientes.service;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.service.UsuarioService;
import fixhub.clientes.dto.ActualizarClienteRequest;
import fixhub.clientes.dto.CrearClienteConCuentaRequest;
import fixhub.clientes.dto.CrearClienteConUsuarioRequest;
import fixhub.clientes.entity.Cliente;
import fixhub.clientes.exception.ClienteNoEncontradoException;
import fixhub.clientes.exception.UsuarioYaVinculadoAClienteException;
import fixhub.clientes.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioService usuarioService;

    private ClienteService clienteService;

    @BeforeEach
    void configurar() {
        clienteService = new ClienteService(
            clienteRepository,
            usuarioService
        );
    }

    @Test
    void debeCrearClienteConUsuarioExistenteYAsegurarRol() {
        Usuario usuario = usuario(7L, "cliente@fixhub.com");
        when(usuarioService.buscarEntidadPorId(7L)).thenReturn(usuario);
        when(clienteRepository.save(any(Cliente.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = clienteService.crearConUsuario(
            new CrearClienteConUsuarioRequest(
                7L, " Ana ", " Pérez ", null, null
            )
        );

        assertThat(response.usuarioId()).isEqualTo(7L);
        assertThat(response.nombre()).isEqualTo("Ana");
        verify(usuarioService).asegurarRolCliente(usuario);
    }

    @Test
    void debeRechazarUsuarioYaVinculado() {
        Usuario usuario = usuario(7L, "cliente@fixhub.com");
        when(usuarioService.buscarEntidadPorId(7L)).thenReturn(usuario);
        when(clienteRepository.existsByUsuarioId(7L)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.crearConUsuario(
            new CrearClienteConUsuarioRequest(
                7L, "Ana", "Pérez", null, null
            )
        )).isInstanceOf(UsuarioYaVinculadoAClienteException.class);

        verify(usuarioService, never()).asegurarRolCliente(any());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void debeCrearClienteConCuentaNuevaControlada() {
        Usuario usuario = usuario(8L, "nuevo@fixhub.com");
        when(usuarioService.crearCuentaCliente(
            "nuevo@fixhub.com", null, "Temporal123"
        )).thenReturn(usuario);
        when(clienteRepository.save(any(Cliente.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = clienteService.crearConCuenta(
            new CrearClienteConCuentaRequest(
                "nuevo@fixhub.com", null, "Temporal123",
                "Juan", "Gómez", null, null
            )
        );

        assertThat(response.usuarioId()).isEqualTo(8L);
        verify(usuarioService).crearCuentaCliente(
            "nuevo@fixhub.com", null, "Temporal123"
        );
    }

    @Test
    void debeListarActualizarYConsultarClientePropio() {
        Usuario usuario = usuario(9L, "propio@fixhub.com");
        Cliente cliente = cliente(3L, usuario);
        when(clienteRepository.findAllByOrderByApellidoAscNombreAscIdAsc())
            .thenReturn(List.of(cliente));
        when(clienteRepository.findById(3L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.findByUsuarioId(9L))
            .thenReturn(Optional.of(cliente));
        when(clienteRepository.save(cliente)).thenReturn(cliente);

        assertThat(clienteService.listar()).hasSize(1);
        assertThat(clienteService.buscarPropio(9L).id()).isEqualTo(3L);
        var actualizado = clienteService.actualizar(
            3L,
            new ActualizarClienteRequest(
                " María ", " López ", " ", " Centro "
            )
        );
        assertThat(actualizado.nombre()).isEqualTo("María");
        assertThat(actualizado.dni()).isNull();
        assertThat(actualizado.direccion()).isEqualTo("Centro");
    }

    @Test
    void debeInformarCuandoUsuarioNoTieneCliente() {
        when(clienteRepository.findByUsuarioId(99L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.buscarPropio(99L))
            .isInstanceOf(ClienteNoEncontradoException.class);
    }

    private Usuario usuario(Long id, String email) {
        Usuario usuario = new Usuario("hash", email, null);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    private Cliente cliente(Long id, Usuario usuario) {
        Cliente cliente = new Cliente(
            usuario, "Ana", "Pérez", "123", "Centro"
        );
        ReflectionTestUtils.setField(cliente, "id", id);
        return cliente;
    }
}
