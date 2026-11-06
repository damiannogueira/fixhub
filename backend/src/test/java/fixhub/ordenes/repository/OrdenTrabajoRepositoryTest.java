package fixhub.ordenes.repository;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.repository.RolRepository;
import fixhub.autenticacion.repository.UsuarioRepository;
import fixhub.clientes.entity.Cliente;
import fixhub.clientes.entity.Equipo;
import fixhub.clientes.repository.ClienteRepository;
import fixhub.clientes.repository.EquipoRepository;
import fixhub.ordenes.entity.EstadoOrden;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.entity.OrdenTrabajo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrdenTrabajoRepositoryTest {

    @Autowired
    private OrdenTrabajoRepository ordenTrabajoRepository;

    @Autowired
    private EstadoOrdenRepository estadoOrdenRepository;

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    private EstadoOrden recibida;
    private Rol tecnicoRol;

    @BeforeEach
    void configurar() {
        recibida = estadoOrdenRepository.saveAndFlush(
            new EstadoOrden(NombreEstadoOrden.RECIBIDA, false)
        );
        tecnicoRol = rolRepository.saveAndFlush(
            new Rol(NombreRol.TECNICO)
        );
    }

    @Test
    void debePersistirEquipoEstadoYTecnicoOpcional() {
        Equipo equipo = guardarEquipo("cliente@fixhub.com");

        OrdenTrabajo orden = ordenTrabajoRepository.saveAndFlush(
            new OrdenTrabajo(
                equipo,
                null,
                recibida,
                "No enciende",
                LocalDateTime.now()
            )
        );

        assertThat(orden.getEquipo()).isEqualTo(equipo);
        assertThat(orden.getEstado()).isEqualTo(recibida);
        assertThat(orden.getTecnico()).isNull();
    }

    @Test
    void debeConsultarOrdenesPorTecnicoAsignado() {
        Equipo equipo = guardarEquipo("asignado@fixhub.com");
        Usuario tecnico = guardarUsuario("tecnico@fixhub.com");
        tecnico.agregarRol(tecnicoRol);
        usuarioRepository.saveAndFlush(tecnico);
        OrdenTrabajo orden = guardarOrden(equipo, tecnico);

        assertThat(
            ordenTrabajoRepository
                .findAllByTecnicoIdOrderByFechaIngresoDescIdDesc(
                    tecnico.getId()
                )
        ).containsExactly(orden);
        assertThat(
            ordenTrabajoRepository.findByIdAndTecnicoId(
                orden.getId(),
                tecnico.getId()
            )
        ).contains(orden);
    }

    @Test
    void debeConsultarSoloOrdenesDelClientePropietario() {
        Usuario propietario = guardarUsuario("propietario@fixhub.com");
        Equipo propio = guardarEquipo(propietario);
        OrdenTrabajo ordenPropia = guardarOrden(propio, null);
        OrdenTrabajo ordenAjena = guardarOrden(
            guardarEquipo("ajeno@fixhub.com"),
            null
        );

        assertThat(
            ordenTrabajoRepository
                .findAllByEquipoClienteUsuarioIdOrderByFechaIngresoDescIdDesc(
                    propietario.getId()
                )
        ).containsExactly(ordenPropia);
        assertThat(
            ordenTrabajoRepository.findByIdAndEquipoClienteUsuarioId(
                ordenAjena.getId(),
                propietario.getId()
            )
        ).isEmpty();
    }

    private OrdenTrabajo guardarOrden(Equipo equipo, Usuario tecnico) {
        return ordenTrabajoRepository.saveAndFlush(
            new OrdenTrabajo(
                equipo,
                tecnico,
                recibida,
                "No enciende",
                LocalDateTime.now()
            )
        );
    }

    private Equipo guardarEquipo(String email) {
        return guardarEquipo(guardarUsuario(email));
    }

    private Equipo guardarEquipo(Usuario usuario) {
        Cliente cliente = clienteRepository.saveAndFlush(
            new Cliente(usuario, "Ana", "Pérez", null, null)
        );
        return equipoRepository.saveAndFlush(
            new Equipo(
                cliente,
                "Notebook",
                "Dell",
                "XPS",
                null,
                null
            )
        );
    }

    private Usuario guardarUsuario(String email) {
        return usuarioRepository.saveAndFlush(
            new Usuario("hash", email, null)
        );
    }
}
