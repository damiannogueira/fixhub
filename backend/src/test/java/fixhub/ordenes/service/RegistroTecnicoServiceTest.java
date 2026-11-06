package fixhub.ordenes.service;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.service.UsuarioService;
import fixhub.ordenes.dto.CrearRegistroTecnicoRequest;
import fixhub.ordenes.dto.RegistroTecnicoResponse;
import fixhub.ordenes.entity.EstadoOrden;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.entity.OrdenTrabajo;
import fixhub.ordenes.entity.RegistroTecnico;
import fixhub.ordenes.exception.EstadoOrdenInvalidoParaRegistroTecnicoException;
import fixhub.ordenes.repository.RegistroTecnicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroTecnicoServiceTest {

    @Mock
    private RegistroTecnicoRepository
        registroTecnicoRepository;

    @Mock
    private OrdenTrabajoService ordenTrabajoService;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private RegistroTecnicoService registroTecnicoService;

    @Test
    void debeCrearRegistroNormalizandoLosDatos() {
        Usuario tecnico = crearTecnico(
            42L,
            "tecnico@fixhub.com"
        );
        OrdenTrabajo orden = crearOrden(
            10L,
            NombreEstadoOrden.EN_REPARACION,
            tecnico
        );

        when(ordenTrabajoService.buscarEntidadPorId(10L))
            .thenReturn(orden);

        when(usuarioService.buscarEntidadPorId(42L))
            .thenReturn(tecnico);

        when(
            registroTecnicoRepository.save(
                any(RegistroTecnico.class)
            )
        ).thenAnswer(invocation -> invocation.getArgument(0));

        RegistroTecnicoResponse response =
            registroTecnicoService.crear(
                10L,
                new CrearRegistroTecnicoRequest(
                    " Reemplazar conector ",
                    " "
                ),
                42L
            );

        assertThat(response.ordenId()).isEqualTo(10L);
        assertThat(response.usuarioId()).isEqualTo(42L);
        assertThat(response.tareaRealizada())
            .isEqualTo("Reemplazar conector");
        assertThat(response.observacion()).isNull();
        assertThat(response.fecha()).isNotNull();

        ArgumentCaptor<RegistroTecnico> captor =
            ArgumentCaptor.forClass(RegistroTecnico.class);

        verify(registroTecnicoRepository)
            .save(captor.capture());

        assertThat(captor.getValue().getOrden())
            .isSameAs(orden);
        assertThat(captor.getValue().getUsuario())
            .isSameAs(tecnico);
    }

    @Test
    void debeNormalizarObservacionInformada() {
        Usuario tecnico = crearTecnico(
            42L,
            "tecnico@fixhub.com"
        );
        OrdenTrabajo orden = crearOrden(
            10L,
            NombreEstadoOrden.EN_REPARACION,
            tecnico
        );

        when(ordenTrabajoService.buscarEntidadPorId(10L))
            .thenReturn(orden);

        when(usuarioService.buscarEntidadPorId(42L))
            .thenReturn(tecnico);

        when(
            registroTecnicoRepository.save(
                any(RegistroTecnico.class)
            )
        ).thenAnswer(invocation -> invocation.getArgument(0));

        RegistroTecnicoResponse response =
            registroTecnicoService.crear(
                10L,
                new CrearRegistroTecnicoRequest(
                    "Reemplazar conector",
                    " El equipo volvió a encender "
                ),
                42L
            );

        assertThat(response.observacion())
            .isEqualTo("El equipo volvió a encender");
    }

    @Test
    void noDebeRegistrarFueraDeReparacion() {
        Usuario tecnico = crearTecnico(
            42L,
            "tecnico@fixhub.com"
        );
        OrdenTrabajo orden = crearOrden(
            10L,
            NombreEstadoOrden.FINALIZADA,
            tecnico
        );

        when(ordenTrabajoService.buscarEntidadPorId(10L))
            .thenReturn(orden);

        when(usuarioService.buscarEntidadPorId(42L))
            .thenReturn(tecnico);

        assertThatThrownBy(
            () -> registroTecnicoService.crear(
                10L,
                new CrearRegistroTecnicoRequest(
                    "Tarea",
                    null
                ),
                42L
            )
        ).isInstanceOf(
            EstadoOrdenInvalidoParaRegistroTecnicoException.class
        );

        verifyNoInteractions(registroTecnicoRepository);
    }

    @Test
    void tecnicoAjenoNoDebeRegistrarTareas() {
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
            NombreEstadoOrden.EN_REPARACION,
            tecnicoAsignado
        );

        when(ordenTrabajoService.buscarEntidadPorId(10L))
            .thenReturn(orden);

        when(usuarioService.buscarEntidadPorId(99L))
            .thenReturn(tecnicoAjeno);

        assertThatThrownBy(
            () -> registroTecnicoService.crear(
                10L,
                new CrearRegistroTecnicoRequest(
                    "Tarea",
                    null
                ),
                99L
            )
        ).isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(registroTecnicoRepository);
    }

    @Test
    void debeListarRegistrosDeUnaOrden() {
        Usuario tecnico = crearTecnico(
            42L,
            "tecnico@fixhub.com"
        );
        OrdenTrabajo orden = crearOrden(
            10L,
            NombreEstadoOrden.FINALIZADA,
            tecnico
        );

        RegistroTecnico primero = crearRegistro(
            100L,
            orden,
            tecnico,
            "Primera tarea",
            11
        );
        RegistroTecnico segundo = crearRegistro(
            101L,
            orden,
            tecnico,
            "Segunda tarea",
            12
        );

        when(ordenTrabajoService.buscarEntidadPorId(10L))
            .thenReturn(orden);

        when(
            registroTecnicoRepository
                .findAllByOrdenIdOrderByFechaAscIdAsc(10L)
        ).thenReturn(List.of(primero, segundo));

        List<RegistroTecnicoResponse> resultado =
            registroTecnicoService.listarPorOrden(10L);

        assertThat(resultado)
            .extracting(
                RegistroTecnicoResponse::tareaRealizada
            )
            .containsExactly(
                "Primera tarea",
                "Segunda tarea"
            );

        verify(ordenTrabajoService)
            .buscarEntidadPorId(10L);
    }

    @Test
    void tecnicoDebeListarSoloRegistrosDeOrdenAsignada() {
        when(
            registroTecnicoRepository
                .findAllByOrdenIdOrderByFechaAscIdAsc(10L)
        ).thenReturn(List.of());

        List<RegistroTecnicoResponse> resultado =
            registroTecnicoService.listarAsignados(
                10L,
                42L
            );

        assertThat(resultado).isEmpty();

        verify(ordenTrabajoService)
            .buscarAsignada(10L, 42L);
    }

    private RegistroTecnico crearRegistro(
        Long id,
        OrdenTrabajo orden,
        Usuario tecnico,
        String tarea,
        int hora
    ) {
        RegistroTecnico registro = new RegistroTecnico(
            orden,
            tecnico,
            tarea,
            null,
            LocalDateTime.of(2026, 11, 6, hora, 0)
        );

        ReflectionTestUtils.setField(
            registro,
            "id",
            id
        );

        return registro;
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
