package fixhub.ordenes.repository;

import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.repository.UsuarioRepository;
import fixhub.clientes.entity.Cliente;
import fixhub.clientes.entity.Equipo;
import fixhub.clientes.repository.ClienteRepository;
import fixhub.clientes.repository.EquipoRepository;
import fixhub.ordenes.entity.EstadoOrden;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.entity.OrdenTrabajo;
import fixhub.ordenes.entity.RegistroTecnico;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RegistroTecnicoRepositoryTest {

    @Autowired
    private RegistroTecnicoRepository registroTecnicoRepository;

    @Autowired
    private OrdenTrabajoRepository ordenTrabajoRepository;

    @Autowired
    private EstadoOrdenRepository estadoOrdenRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EquipoRepository equipoRepository;

    @Test
    void debePersistirRegistroConOrdenYTecnico() {
        OrdenTrabajo orden = crearOrden();

        RegistroTecnico registro =
            registroTecnicoRepository.saveAndFlush(
                new RegistroTecnico(
                    orden,
                    orden.getTecnico(),
                    "Se reemplazó el conector",
                    "El equipo volvió a encender",
                    LocalDateTime.of(2026, 11, 6, 13, 0)
                )
            );

        assertThat(registro.getId()).isNotNull();
        assertThat(registro.getOrden().getId())
            .isEqualTo(orden.getId());
        assertThat(registro.getUsuario().getId())
            .isEqualTo(orden.getTecnico().getId());
        assertThat(registro.getTareaRealizada())
            .isEqualTo("Se reemplazó el conector");
        assertThat(registro.getObservacion())
            .isEqualTo("El equipo volvió a encender");
    }

    @Test
    void debeListarRegistrosDeUnaOrdenCronologicamente() {
        OrdenTrabajo orden = crearOrden();

        OrdenTrabajo otraOrden = ordenTrabajoRepository.saveAndFlush(
            new OrdenTrabajo(
                orden.getEquipo(),
                orden.getTecnico(),
                orden.getEstado(),
                "Otro problema",
                LocalDateTime.of(2026, 11, 6, 9, 30)
            )
        );

        RegistroTecnico segundo = crearRegistro(
            orden,
            "Segunda tarea",
            12
        );
        RegistroTecnico primero = crearRegistro(
            orden,
            "Primera tarea",
            11
        );
        RegistroTecnico tercero = crearRegistro(
            orden,
            "Tercera tarea",
            13
        );
        RegistroTecnico registroAjeno = crearRegistro(
            otraOrden,
            "Tarea de otra orden",
            10
        );

        registroTecnicoRepository.saveAllAndFlush(
            List.of(
                segundo,
                primero,
                tercero,
                registroAjeno
            )
        );

        List<RegistroTecnico> resultado =
            registroTecnicoRepository
                .findAllByOrdenIdOrderByFechaAscIdAsc(
                    orden.getId()
                );

        assertThat(resultado)
            .extracting(RegistroTecnico::getTareaRealizada)
            .containsExactly(
                "Primera tarea",
                "Segunda tarea",
                "Tercera tarea"
            );
    }

    private RegistroTecnico crearRegistro(
        OrdenTrabajo orden,
        String tarea,
        int hora
    ) {
        return new RegistroTecnico(
            orden,
            orden.getTecnico(),
            tarea,
            null,
            LocalDateTime.of(2026, 11, 6, hora, 0)
        );
    }

    private OrdenTrabajo crearOrden() {
        Usuario tecnico = usuarioRepository.save(
            new Usuario(
                "hash-tecnico",
                "tecnico-registro@fixhub.com",
                null
            )
        );

        Usuario usuarioCliente = usuarioRepository.save(
            new Usuario(
                "hash-cliente",
                "cliente-registro@fixhub.com",
                null
            )
        );

        Cliente cliente = clienteRepository.save(
            new Cliente(
                usuarioCliente,
                "Ana",
                "Pérez",
                null,
                null
            )
        );

        Equipo equipo = equipoRepository.save(
            new Equipo(
                cliente,
                "Notebook",
                "Dell",
                "XPS",
                "SERIE-REGISTRO",
                null
            )
        );

        EstadoOrden estado = estadoOrdenRepository.save(
            new EstadoOrden(
                NombreEstadoOrden.EN_REPARACION,
                false
            )
        );

        return ordenTrabajoRepository.saveAndFlush(
            new OrdenTrabajo(
                equipo,
                tecnico,
                estado,
                "No enciende",
                LocalDateTime.of(2026, 11, 6, 9, 0)
            )
        );
    }
}
