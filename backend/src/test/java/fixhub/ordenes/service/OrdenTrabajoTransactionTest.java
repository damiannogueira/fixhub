package fixhub.ordenes.service;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.exception.UsuarioNoEncontradoException;
import fixhub.autenticacion.repository.RolRepository;
import fixhub.autenticacion.repository.UsuarioRepository;
import fixhub.clientes.entity.Cliente;
import fixhub.clientes.entity.Equipo;
import fixhub.clientes.repository.ClienteRepository;
import fixhub.clientes.repository.EquipoRepository;
import fixhub.ordenes.dto.CrearOrdenTrabajoRequest;
import fixhub.ordenes.entity.EstadoOrden;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.repository.EstadoOrdenRepository;
import fixhub.ordenes.repository.OrdenTrabajoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrdenTrabajoTransactionTest {

    @Autowired
    private OrdenTrabajoService ordenTrabajoService;

    @Autowired
    private OrdenTrabajoRepository ordenTrabajoRepository;

    @Autowired
    private EstadoOrdenRepository estadoOrdenRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EquipoRepository equipoRepository;

    @Test
    void fallaDeHistorialNoDebeDejarOrdenCreada() {
        estadoOrdenRepository.save(
            new EstadoOrden(NombreEstadoOrden.RECIBIDA, false)
        );
        Rol rolTecnico = rolRepository.save(new Rol(NombreRol.TECNICO));
        Usuario tecnico = new Usuario("hash", "tecnico@fixhub.com", null);
        tecnico.agregarRol(rolTecnico);
        usuarioRepository.save(tecnico);
        Usuario clienteUsuario = usuarioRepository.save(
            new Usuario("hash", "cliente@fixhub.com", null)
        );
        Cliente cliente = clienteRepository.save(
            new Cliente(clienteUsuario, "Ana", "Pérez", null, null)
        );
        Equipo equipo = equipoRepository.save(
            new Equipo(
                cliente,
                "Notebook",
                "Dell",
                "XPS",
                null,
                null
            )
        );
        long ordenesIniciales = ordenTrabajoRepository.count();

        assertThatThrownBy(() -> ordenTrabajoService.crear(
            new CrearOrdenTrabajoRequest(
                equipo.getId(),
                tecnico.getId(),
                "No enciende"
            ),
            999L
        )).isInstanceOf(UsuarioNoEncontradoException.class);

        assertThat(ordenTrabajoRepository.count())
            .isEqualTo(ordenesIniciales);
    }
}
