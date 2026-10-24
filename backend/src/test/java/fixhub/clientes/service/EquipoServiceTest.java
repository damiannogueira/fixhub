package fixhub.clientes.service;

import fixhub.autenticacion.entity.Usuario;
import fixhub.clientes.dto.ActualizarEquipoRequest;
import fixhub.clientes.dto.CrearEquipoRequest;
import fixhub.clientes.entity.Cliente;
import fixhub.clientes.entity.Equipo;
import fixhub.clientes.exception.ClienteNoEncontradoException;
import fixhub.clientes.exception.EquipoNoEncontradoException;
import fixhub.clientes.repository.EquipoRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipoServiceTest {

    @Mock
    private EquipoRepository equipoRepository;

    @Mock
    private ClienteService clienteService;

    private EquipoService equipoService;

    @BeforeEach
    void configurar() {
        equipoService = new EquipoService(
            equipoRepository,
            clienteService
        );
    }

    @Test
    void debeCrearEquipoParaCliente() {
        Cliente cliente = cliente(3L, 9L);
        when(clienteService.buscarEntidadPorId(3L)).thenReturn(cliente);
        when(equipoRepository.save(any(Equipo.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = equipoService.crear(
            3L,
            new CrearEquipoRequest(
                " Notebook ", " Dell ", " XPS ", null, null
            )
        );

        assertThat(response.clienteId()).isEqualTo(3L);
        assertThat(response.tipo()).isEqualTo("Notebook");
        assertThat(response.marca()).isEqualTo("Dell");
    }

    @Test
    void debePropagarClienteInexistente() {
        when(clienteService.buscarEntidadPorId(77L))
            .thenThrow(new ClienteNoEncontradoException(77L));

        assertThatThrownBy(() -> equipoService.crear(
            77L,
            new CrearEquipoRequest("Celular", null, null, null, null)
        )).isInstanceOf(ClienteNoEncontradoException.class);
    }

    @Test
    void debeListarYActualizarEquipo() {
        Cliente cliente = cliente(3L, 9L);
        Equipo equipo = equipo(5L, cliente);
        when(clienteService.buscarEntidadPorId(3L)).thenReturn(cliente);
        when(equipoRepository.findAllByClienteIdOrderByIdAsc(3L))
            .thenReturn(List.of(equipo));
        when(equipoRepository.findById(5L)).thenReturn(Optional.of(equipo));
        when(equipoRepository.save(equipo)).thenReturn(equipo);

        assertThat(equipoService.listarPorCliente(3L)).hasSize(1);
        var actualizado = equipoService.actualizar(
            5L,
            new ActualizarEquipoRequest(
                "Celular", "Samsung", "S24", " ", " Revisado "
            )
        );
        assertThat(actualizado.tipo()).isEqualTo("Celular");
        assertThat(actualizado.numeroSerie()).isNull();
        assertThat(actualizado.observaciones()).isEqualTo("Revisado");
    }

    @Test
    void debeConsultarEquiposPropios() {
        Cliente cliente = cliente(3L, 9L);
        Equipo equipo = equipo(5L, cliente);
        when(clienteService.buscarEntidadPorUsuarioId(9L))
            .thenReturn(cliente);
        when(equipoRepository.findAllByClienteIdOrderByIdAsc(3L))
            .thenReturn(List.of(equipo));
        when(equipoRepository.findByIdAndClienteUsuarioId(5L, 9L))
            .thenReturn(Optional.of(equipo));

        assertThat(equipoService.listarPropios(9L)).hasSize(1);
        assertThat(equipoService.buscarPropio(5L, 9L).id())
            .isEqualTo(5L);
    }

    @Test
    void equipoAjenoDebeResponderComoNoEncontrado() {
        when(equipoRepository.findByIdAndClienteUsuarioId(5L, 99L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipoService.buscarPropio(5L, 99L))
            .isInstanceOf(EquipoNoEncontradoException.class);
    }

    private Cliente cliente(Long clienteId, Long usuarioId) {
        Usuario usuario = new Usuario("hash", "cliente@fixhub.com", null);
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
        Cliente cliente = new Cliente(
            usuario, "Ana", "Pérez", null, null
        );
        ReflectionTestUtils.setField(cliente, "id", clienteId);
        return cliente;
    }

    private Equipo equipo(Long id, Cliente cliente) {
        Equipo equipo = new Equipo(
            cliente, "Notebook", "Dell", "XPS", null, null
        );
        ReflectionTestUtils.setField(equipo, "id", id);
        return equipo;
    }
}
