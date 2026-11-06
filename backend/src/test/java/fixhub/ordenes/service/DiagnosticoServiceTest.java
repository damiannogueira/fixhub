package fixhub.ordenes.service;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.service.UsuarioService;
import fixhub.ordenes.dto.ActualizarDiagnosticoRequest;
import fixhub.ordenes.dto.CrearDiagnosticoRequest;
import fixhub.ordenes.dto.DiagnosticoResponse;
import fixhub.ordenes.entity.Diagnostico;
import fixhub.ordenes.entity.EstadoOrden;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.entity.OrdenTrabajo;
import fixhub.ordenes.exception.DiagnosticoNoEncontradoException;
import fixhub.ordenes.exception.DiagnosticoYaRegistradoException;
import fixhub.ordenes.exception.EstadoOrdenInvalidoParaDiagnosticoException;
import fixhub.ordenes.repository.DiagnosticoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosticoServiceTest {

    @Mock
    private DiagnosticoRepository diagnosticoRepository;

    @Mock
    private OrdenTrabajoService ordenTrabajoService;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private DiagnosticoService diagnosticoService;

    @Test
    void debeCrearDiagnosticoNormalizandoLosDatos() {
        Usuario tecnico = crearTecnico(
            42L,
            "tecnico@fixhub.com"
        );
        OrdenTrabajo orden = crearOrden(
            10L,
            NombreEstadoOrden.EN_DIAGNOSTICO,
            tecnico
        );

        CrearDiagnosticoRequest request =
            new CrearDiagnosticoRequest(
                " Falla en la alimentación ",
                " Reemplazar conector "
            );

        prepararBusqueda(orden, tecnico);

        when(diagnosticoRepository.existsByOrdenId(10L))
            .thenReturn(false);

        when(diagnosticoRepository.save(any(Diagnostico.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        DiagnosticoResponse response =
            diagnosticoService.crear(10L, request, 42L);

        assertThat(response.ordenId()).isEqualTo(10L);
        assertThat(response.usuarioId()).isEqualTo(42L);
        assertThat(response.descripcionFalla())
            .isEqualTo("Falla en la alimentación");
        assertThat(response.tareasRequeridas())
            .isEqualTo("Reemplazar conector");
        assertThat(response.fecha()).isNotNull();

        ArgumentCaptor<Diagnostico> captor =
            ArgumentCaptor.forClass(Diagnostico.class);

        verify(diagnosticoRepository).save(captor.capture());

        assertThat(captor.getValue().getOrden()).isSameAs(orden);
        assertThat(captor.getValue().getUsuario())
            .isSameAs(tecnico);
    }

    @Test
    void noDebeCrearDosDiagnosticosParaLaMismaOrden() {
        Usuario tecnico = crearTecnico(
            42L,
            "tecnico@fixhub.com"
        );
        OrdenTrabajo orden = crearOrden(
            10L,
            NombreEstadoOrden.EN_DIAGNOSTICO,
            tecnico
        );

        prepararBusqueda(orden, tecnico);

        when(diagnosticoRepository.existsByOrdenId(10L))
            .thenReturn(true);

        CrearDiagnosticoRequest request =
            new CrearDiagnosticoRequest(
                "Falla",
                "Reparación"
            );

        assertThatThrownBy(
            () -> diagnosticoService.crear(
                10L,
                request,
                42L
            )
        )
            .isInstanceOf(
                DiagnosticoYaRegistradoException.class
            )
            .hasMessageContaining("10");

        verify(diagnosticoRepository, never())
            .save(any(Diagnostico.class));
    }

    @Test
    void debeActualizarDiagnosticoExistente() {
        Usuario tecnico = crearTecnico(
            42L,
            "tecnico@fixhub.com"
        );
        OrdenTrabajo orden = crearOrden(
            10L,
            NombreEstadoOrden.EN_DIAGNOSTICO,
            tecnico
        );

        LocalDateTime fechaAnterior =
            LocalDateTime.of(2026, 11, 6, 10, 0);

        Diagnostico diagnostico = new Diagnostico(
            orden,
            tecnico,
            "Falla anterior",
            "Tarea anterior",
            fechaAnterior
        );

        ReflectionTestUtils.setField(
            diagnostico,
            "id",
            100L
        );

        prepararBusqueda(orden, tecnico);

        when(diagnosticoRepository.findByOrdenId(10L))
            .thenReturn(Optional.of(diagnostico));

        when(diagnosticoRepository.save(diagnostico))
            .thenReturn(diagnostico);

        DiagnosticoResponse response =
            diagnosticoService.actualizar(
                10L,
                new ActualizarDiagnosticoRequest(
                    " Nueva falla ",
                    " Nueva tarea "
                ),
                42L
            );

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.descripcionFalla())
            .isEqualTo("Nueva falla");
        assertThat(response.tareasRequeridas())
            .isEqualTo("Nueva tarea");
        assertThat(response.fecha()).isAfter(fechaAnterior);

        verify(diagnosticoRepository).save(diagnostico);
    }

    @Test
    void noDebeActualizarDiagnosticoInexistente() {
        Usuario tecnico = crearTecnico(
            42L,
            "tecnico@fixhub.com"
        );
        OrdenTrabajo orden = crearOrden(
            10L,
            NombreEstadoOrden.EN_DIAGNOSTICO,
            tecnico
        );

        prepararBusqueda(orden, tecnico);

        when(diagnosticoRepository.findByOrdenId(10L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> diagnosticoService.actualizar(
                10L,
                new ActualizarDiagnosticoRequest(
                    "Falla",
                    "Tarea"
                ),
                42L
            )
        )
            .isInstanceOf(
                DiagnosticoNoEncontradoException.class
            )
            .hasMessageContaining("10");

        verify(diagnosticoRepository, never())
            .save(any(Diagnostico.class));
    }

    @Test
    void noDebeGestionarDiagnosticoFueraDeDiagnostico() {
        Usuario tecnico = crearTecnico(
            42L,
            "tecnico@fixhub.com"
        );
        OrdenTrabajo orden = crearOrden(
            10L,
            NombreEstadoOrden.EN_REPARACION,
            tecnico
        );

        prepararBusqueda(orden, tecnico);

        CrearDiagnosticoRequest request =
            new CrearDiagnosticoRequest(
                "Falla",
                "Tarea"
            );

        assertThatThrownBy(
            () -> diagnosticoService.crear(
                10L,
                request,
                42L
            )
        ).isInstanceOf(
            EstadoOrdenInvalidoParaDiagnosticoException.class
        );

        verifyNoInteractions(diagnosticoRepository);
    }

    @Test
    void tecnicoAjenoNoDebeGestionarDiagnostico() {
        Usuario tecnicoAsignado = crearTecnico(
            42L,
            "asignado@fixhub.com"
        );
        Usuario tecnicoAjeno = crearTecnico(
            99L,
            "ajeno@fixhub.com"
        );
        OrdenTrabajo orden = crearOrden(
            10L,
            NombreEstadoOrden.EN_DIAGNOSTICO,
            tecnicoAsignado
        );

        when(ordenTrabajoService.buscarEntidadPorId(10L))
            .thenReturn(orden);

        when(usuarioService.buscarEntidadPorId(99L))
            .thenReturn(tecnicoAjeno);

        CrearDiagnosticoRequest request =
            new CrearDiagnosticoRequest(
                "Falla",
                "Tarea"
            );

        assertThatThrownBy(
            () -> diagnosticoService.crear(
                10L,
                request,
                99L
            )
        ).isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(diagnosticoRepository);
    }

    @Test
    void tecnicoDebeConsultarSoloDiagnosticoDeOrdenAsignada() {
        Usuario tecnico = crearTecnico(
            42L,
            "tecnico@fixhub.com"
        );
        OrdenTrabajo orden = crearOrden(
            10L,
            NombreEstadoOrden.EN_REPARACION,
            tecnico
        );

        Diagnostico diagnostico = new Diagnostico(
            orden,
            tecnico,
            "Falla diagnosticada",
            "Reparación necesaria",
            LocalDateTime.of(2026, 11, 6, 12, 0)
        );

        ReflectionTestUtils.setField(
            diagnostico,
            "id",
            100L
        );

        when(ordenTrabajoService.buscarEntidadPorId(10L))
            .thenReturn(orden);

        when(diagnosticoRepository.findByOrdenId(10L))
            .thenReturn(Optional.of(diagnostico));

        DiagnosticoResponse response =
            diagnosticoService.buscarAsignado(10L, 42L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.ordenId()).isEqualTo(10L);
        assertThat(response.usuarioId()).isEqualTo(42L);

        verify(ordenTrabajoService)
            .buscarAsignada(10L, 42L);
    }

    @Test
    void clienteDebeConsultarDiagnosticoSinDatosInternos() {
        Usuario tecnico = crearTecnico(
            42L,
            "tecnico@fixhub.com"
        );
        OrdenTrabajo orden = crearOrden(
            10L,
            NombreEstadoOrden.FINALIZADA,
            tecnico
        );

        Diagnostico diagnostico = new Diagnostico(
            orden,
            tecnico,
            "Falla diagnosticada",
            "Reparación necesaria",
            LocalDateTime.of(2026, 11, 6, 12, 0)
        );

        ReflectionTestUtils.setField(
            diagnostico,
            "id",
            100L
        );

        when(diagnosticoRepository.findByOrdenId(10L))
            .thenReturn(Optional.of(diagnostico));

        var response =
            diagnosticoService.buscarPropioCliente(10L, 77L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.ordenId()).isEqualTo(10L);
        assertThat(response.descripcionFalla())
            .isEqualTo("Falla diagnosticada");
        assertThat(response.tareasRequeridas())
            .isEqualTo("Reparación necesaria");

        verify(ordenTrabajoService)
            .buscarPropiaCliente(10L, 77L);
    }

    private void prepararBusqueda(
        OrdenTrabajo orden,
        Usuario tecnico
    ) {
        when(
            ordenTrabajoService.buscarEntidadPorId(
                orden.getId()
            )
        ).thenReturn(orden);

        when(
            usuarioService.buscarEntidadPorId(
                tecnico.getId()
            )
        ).thenReturn(tecnico);
    }

    private Usuario crearTecnico(Long id, String email) {
        Usuario tecnico = new Usuario(
            "hash",
            email,
            null
        );

        tecnico.agregarRol(
            new Rol(NombreRol.TECNICO)
        );

        ReflectionTestUtils.setField(
            tecnico,
            "id",
            id
        );

        return tecnico;
    }

    private OrdenTrabajo crearOrden(
        Long id,
        NombreEstadoOrden estado,
        Usuario tecnico
    ) {
        OrdenTrabajo orden = new OrdenTrabajo(
            null,
            tecnico,
            new EstadoOrden(estado, false),
            "No enciende",
            LocalDateTime.of(2026, 11, 6, 9, 0)
        );

        ReflectionTestUtils.setField(
            orden,
            "id",
            id
        );

        return orden;
    }
}
