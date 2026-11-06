package fixhub.ordenes.repository;

import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.repository.UsuarioRepository;
import fixhub.clientes.entity.Cliente;
import fixhub.clientes.entity.Equipo;
import fixhub.clientes.repository.ClienteRepository;
import fixhub.clientes.repository.EquipoRepository;
import fixhub.ordenes.entity.Diagnostico;
import fixhub.ordenes.entity.EstadoOrden;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.entity.OrdenTrabajo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class DiagnosticoRepositoryTest {

    @Autowired
    private DiagnosticoRepository diagnosticoRepository;

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
    void debePersistirDiagnosticoAsociadoAOrdenYTecnico() {
        OrdenTrabajo orden = crearOrden();
        LocalDateTime fecha = LocalDateTime.of(
            2026,
            11,
            6,
            12,
            0
        );

        Diagnostico diagnostico = diagnosticoRepository.saveAndFlush(
            new Diagnostico(
                orden,
                orden.getTecnico(),
                "Falla en el circuito de alimentación",
                "Reemplazar conector y comprobar voltajes",
                fecha
            )
        );

        var resultado =
            diagnosticoRepository.findByOrdenId(orden.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId())
            .isEqualTo(diagnostico.getId());
        assertThat(resultado.get().getOrden().getId())
            .isEqualTo(orden.getId());
        assertThat(resultado.get().getUsuario().getId())
            .isEqualTo(orden.getTecnico().getId());
        assertThat(resultado.get().getDescripcionFalla())
            .isEqualTo("Falla en el circuito de alimentación");
        assertThat(resultado.get().getTareasRequeridas())
            .isEqualTo(
                "Reemplazar conector y comprobar voltajes"
            );
        assertThat(resultado.get().getFecha()).isEqualTo(fecha);
    }

    @Test
    void debeDetectarSiLaOrdenYaTieneDiagnostico() {
        OrdenTrabajo orden = crearOrden();

        diagnosticoRepository.saveAndFlush(
            crearDiagnostico(orden, "Falla detectada")
        );

        assertThat(
            diagnosticoRepository.existsByOrdenId(orden.getId())
        ).isTrue();
    }

    @Test
    void noDebePermitirDosDiagnosticosParaLaMismaOrden() {
        OrdenTrabajo orden = crearOrden();

        diagnosticoRepository.saveAndFlush(
            crearDiagnostico(orden, "Primera falla")
        );

        Diagnostico segundoDiagnostico =
            crearDiagnostico(orden, "Segunda falla");

        assertThatThrownBy(
            () -> diagnosticoRepository.saveAndFlush(
                segundoDiagnostico
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Diagnostico crearDiagnostico(
        OrdenTrabajo orden,
        String descripcion
    ) {
        return new Diagnostico(
            orden,
            orden.getTecnico(),
            descripcion,
            "Realizar reparación",
            LocalDateTime.of(2026, 11, 6, 12, 0)
        );
    }

    private OrdenTrabajo crearOrden() {
        Usuario tecnico = usuarioRepository.save(
            new Usuario(
                "hash-tecnico",
                "tecnico-diagnostico@fixhub.com",
                null
            )
        );

        Usuario usuarioCliente = usuarioRepository.save(
            new Usuario(
                "hash-cliente",
                "cliente-diagnostico@fixhub.com",
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
                "SERIE-DIAGNOSTICO",
                null
            )
        );

        EstadoOrden estado = estadoOrdenRepository.save(
            new EstadoOrden(
                NombreEstadoOrden.EN_DIAGNOSTICO,
                false
            )
        );

        return ordenTrabajoRepository.saveAndFlush(
            new OrdenTrabajo(
                equipo,
                tecnico,
                estado,
                "No enciende",
                LocalDateTime.of(2026, 11, 6, 10, 0)
            )
        );
    }
}
