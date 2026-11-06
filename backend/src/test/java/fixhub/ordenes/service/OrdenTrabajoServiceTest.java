package fixhub.ordenes.service;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.service.UsuarioService;
import fixhub.clientes.entity.Cliente;
import fixhub.clientes.entity.Equipo;
import fixhub.clientes.service.EquipoService;
import fixhub.ordenes.dto.CrearOrdenTrabajoRequest;
import fixhub.ordenes.entity.EstadoOrden;
import fixhub.ordenes.entity.HistorialOrden;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.entity.OrdenTrabajo;
import fixhub.ordenes.entity.TipoEventoOrden;
import fixhub.ordenes.exception.OrdenNoCancelableException;
import fixhub.ordenes.exception.TecnicoRequeridoException;
import fixhub.ordenes.exception.TransicionEstadoInvalidaException;
import fixhub.ordenes.repository.EstadoOrdenRepository;
import fixhub.ordenes.repository.HistorialOrdenRepository;
import fixhub.ordenes.repository.OrdenTrabajoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenTrabajoServiceTest {

    @Mock
    private OrdenTrabajoRepository ordenTrabajoRepository;

    @Mock
    private EstadoOrdenRepository estadoOrdenRepository;

    @Mock
    private HistorialOrdenRepository historialOrdenRepository;

    @Mock
    private EquipoService equipoService;

    @Mock
    private UsuarioService usuarioService;

    private OrdenTrabajoService ordenTrabajoService;

    @BeforeEach
    void configurar() {
        ordenTrabajoService = new OrdenTrabajoService(
            ordenTrabajoRepository,
            estadoOrdenRepository,
            historialOrdenRepository,
            equipoService,
            usuarioService
        );
    }

    @Test
    void debeCrearSiempreEnRecibidaSinTecnico() {
        Equipo equipo = equipo(3L, usuario(10L, NombreRol.CLIENTE));
        EstadoOrden recibida = estado(NombreEstadoOrden.RECIBIDA);
        when(equipoService.buscarEntidadPorId(3L)).thenReturn(equipo);
        when(estadoOrdenRepository.findByNombre(NombreEstadoOrden.RECIBIDA))
            .thenReturn(Optional.of(recibida));
        when(ordenTrabajoRepository.save(any(OrdenTrabajo.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = ordenTrabajoService.crear(
            new CrearOrdenTrabajoRequest(3L, null, " No enciende "),
            20L
        );

        assertThat(response.estado()).isEqualTo(NombreEstadoOrden.RECIBIDA);
        assertThat(response.tecnicoId()).isNull();
        assertThat(response.problemaInicial()).isEqualTo("No enciende");
        assertThat(response.fechaIngreso()).isNotNull();
    }

    @Test
    void debeCrearConTecnicoYRegistrarAsignacion() {
        Usuario actor = usuario(20L, NombreRol.RECEPCIONISTA);
        Usuario tecnico = usuario(30L, NombreRol.TECNICO);
        Equipo equipo = equipo(3L, usuario(10L, NombreRol.CLIENTE));
        when(equipoService.buscarEntidadPorId(3L)).thenReturn(equipo);
        when(usuarioService.buscarTecnicoPorId(30L)).thenReturn(tecnico);
        when(usuarioService.buscarEntidadPorId(20L)).thenReturn(actor);
        when(estadoOrdenRepository.findByNombre(NombreEstadoOrden.RECIBIDA))
            .thenReturn(Optional.of(estado(NombreEstadoOrden.RECIBIDA)));
        when(ordenTrabajoRepository.save(any(OrdenTrabajo.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = ordenTrabajoService.crear(
            new CrearOrdenTrabajoRequest(3L, 30L, "No enciende"),
            20L
        );

        assertThat(response.tecnicoId()).isEqualTo(30L);
        HistorialOrden historial = historialGuardado();
        assertThat(historial.getTipoEvento())
            .isEqualTo(TipoEventoOrden.ASIGNACION_TECNICO);
        assertThat(historial.isVisibleCliente()).isFalse();
    }

    @Test
    void debeAsignarYCambiarTecnicoConHistorialInterno() {
        Usuario actor = usuario(20L, NombreRol.RECEPCIONISTA);
        Usuario anterior = usuario(30L, NombreRol.TECNICO);
        Usuario nuevo = usuario(31L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(
            7L,
            NombreEstadoOrden.RECIBIDA,
            anterior
        );
        when(ordenTrabajoRepository.findById(7L))
            .thenReturn(Optional.of(orden));
        when(usuarioService.buscarTecnicoPorId(31L)).thenReturn(nuevo);
        when(usuarioService.buscarEntidadPorId(20L)).thenReturn(actor);

        var response = ordenTrabajoService.asignarTecnico(7L, 31L, 20L);

        assertThat(response.tecnicoId()).isEqualTo(31L);
        HistorialOrden historial = historialGuardado();
        assertThat(historial.getTipoEvento())
            .isEqualTo(TipoEventoOrden.CAMBIO_TECNICO);
        assertThat(historial.getComentario())
            .contains("30", "31");
        assertThat(historial.isVisibleCliente()).isFalse();
    }

    @Test
    void debeRegistrarAsignacionPosterior() {
        Usuario actor = usuario(20L, NombreRol.RECEPCIONISTA);
        Usuario tecnico = usuario(30L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.RECIBIDA, null);
        when(ordenTrabajoRepository.findById(7L))
            .thenReturn(Optional.of(orden));
        when(usuarioService.buscarTecnicoPorId(30L)).thenReturn(tecnico);
        when(usuarioService.buscarEntidadPorId(20L)).thenReturn(actor);

        ordenTrabajoService.asignarTecnico(7L, 30L, 20L);

        assertThat(historialGuardado().getTipoEvento())
            .isEqualTo(TipoEventoOrden.ASIGNACION_TECNICO);
    }

    @Test
    void reasignarMismoTecnicoNoDebeCrearEvento() {
        Usuario tecnico = usuario(30L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(
            7L,
            NombreEstadoOrden.RECIBIDA,
            tecnico
        );
        when(ordenTrabajoRepository.findById(7L))
            .thenReturn(Optional.of(orden));
        when(usuarioService.buscarTecnicoPorId(30L)).thenReturn(tecnico);

        ordenTrabajoService.asignarTecnico(7L, 30L, 20L);

        verify(historialOrdenRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "RECIBIDA:EN_DIAGNOSTICO",
        "EN_DIAGNOSTICO:EN_REPARACION",
        "APROBADA:EN_REPARACION",
        "EN_REPARACION:FINALIZADA"
    })
    void tecnicoAsignadoDebeRealizarTransicionesTecnicas(String cambio) {
        String[] estados = cambio.split(":");
        NombreEstadoOrden origen = NombreEstadoOrden.valueOf(estados[0]);
        NombreEstadoOrden destino = NombreEstadoOrden.valueOf(estados[1]);
        Usuario tecnico = usuario(30L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(7L, origen, tecnico);
        prepararCambioEstado(orden, tecnico, destino);

        var response = ordenTrabajoService.cambiarEstado(7L, destino, 30L);

        assertThat(response.estado()).isEqualTo(destino);
        HistorialOrden historial = historialGuardado();
        assertThat(historial.getTipoEvento())
            .isEqualTo(TipoEventoOrden.CAMBIO_ESTADO);
        assertThat(historial.getEstadoAnterior().getNombre())
            .isEqualTo(origen);
        assertThat(historial.getEstadoNuevo().getNombre())
            .isEqualTo(destino);
        assertThat(historial.isVisibleCliente()).isTrue();
    }

    @Test
    void noDebeIniciarDiagnosticoSinTecnico() {
        Usuario actor = usuario(30L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.RECIBIDA, null);
        when(ordenTrabajoRepository.findById(7L))
            .thenReturn(Optional.of(orden));
        when(usuarioService.buscarEntidadPorId(30L)).thenReturn(actor);

        assertThatThrownBy(() -> ordenTrabajoService.cambiarEstado(
            7L,
            NombreEstadoOrden.EN_DIAGNOSTICO,
            30L
        )).isInstanceOf(TecnicoRequeridoException.class);
    }

    @Test
    void tecnicoAjenoNoDebeCambiarEstado() {
        Usuario asignado = usuario(30L, NombreRol.TECNICO);
        Usuario ajeno = usuario(31L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(
            7L,
            NombreEstadoOrden.RECIBIDA,
            asignado
        );
        when(ordenTrabajoRepository.findById(7L))
            .thenReturn(Optional.of(orden));
        when(usuarioService.buscarEntidadPorId(31L)).thenReturn(ajeno);

        assertThatThrownBy(() -> ordenTrabajoService.cambiarEstado(
            7L,
            NombreEstadoOrden.EN_DIAGNOSTICO,
            31L
        )).isInstanceOf(AccessDeniedException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "FINALIZADA:LISTA_PARA_RETIRAR",
        "LISTA_PARA_RETIRAR:ENTREGADA"
    })
    void recepcionistaDebeCompletarEntrega(String cambio) {
        String[] estados = cambio.split(":");
        NombreEstadoOrden origen = NombreEstadoOrden.valueOf(estados[0]);
        NombreEstadoOrden destino = NombreEstadoOrden.valueOf(estados[1]);
        Usuario recepcionista = usuario(20L, NombreRol.RECEPCIONISTA);
        OrdenTrabajo orden = orden(7L, origen, null);
        prepararCambioEstado(orden, recepcionista, destino);

        var response = ordenTrabajoService.cambiarEstado(7L, destino, 20L);

        assertThat(response.estado()).isEqualTo(destino);
        if (destino == NombreEstadoOrden.ENTREGADA) {
            assertThat(response.fechaEntrega()).isNotNull();
        }
    }

    @Test
    void tecnicoNoDebeRealizarTransicionDeRecepcion() {
        Usuario tecnico = usuario(30L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(
            7L,
            NombreEstadoOrden.FINALIZADA,
            tecnico
        );
        when(ordenTrabajoRepository.findById(7L))
            .thenReturn(Optional.of(orden));
        when(usuarioService.buscarEntidadPorId(30L)).thenReturn(tecnico);

        assertThatThrownBy(() -> ordenTrabajoService.cambiarEstado(
            7L,
            NombreEstadoOrden.LISTA_PARA_RETIRAR,
            30L
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void recepcionistaNoDebeRealizarTransicionTecnica() {
        Usuario tecnico = usuario(30L, NombreRol.TECNICO);
        Usuario recepcionista = usuario(20L, NombreRol.RECEPCIONISTA);
        OrdenTrabajo orden = orden(
            7L,
            NombreEstadoOrden.RECIBIDA,
            tecnico
        );
        when(ordenTrabajoRepository.findById(7L))
            .thenReturn(Optional.of(orden));
        when(usuarioService.buscarEntidadPorId(20L))
            .thenReturn(recepcionista);

        assertThatThrownBy(() -> ordenTrabajoService.cambiarEstado(
            7L,
            NombreEstadoOrden.EN_DIAGNOSTICO,
            20L
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void noDebeHabilitarTransicionesReservadasParaPresupuesto() {
        Usuario tecnico = usuario(30L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(
            7L,
            NombreEstadoOrden.EN_DIAGNOSTICO,
            tecnico
        );
        when(ordenTrabajoRepository.findById(7L))
            .thenReturn(Optional.of(orden));
        when(usuarioService.buscarEntidadPorId(30L)).thenReturn(tecnico);

        assertThatThrownBy(() -> ordenTrabajoService.cambiarEstado(
            7L,
            NombreEstadoOrden.PENDIENTE_APROBACION,
            30L
        )).isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void estadoTerminalNoDebeTenerSalida() {
        Usuario recepcionista = usuario(20L, NombreRol.RECEPCIONISTA);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.ENTREGADA, null);
        when(ordenTrabajoRepository.findById(7L))
            .thenReturn(Optional.of(orden));
        when(usuarioService.buscarEntidadPorId(20L))
            .thenReturn(recepcionista);

        assertThatThrownBy(() -> ordenTrabajoService.cambiarEstado(
            7L,
            NombreEstadoOrden.RECIBIDA,
            20L
        )).isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @ParameterizedTest
    @EnumSource(
        value = NombreEstadoOrden.class,
        names = {
            "RECIBIDA",
            "EN_DIAGNOSTICO",
            "PENDIENTE_APROBACION",
            "APROBADA"
        }
    )
    void administradorDebeCancelarEstadosPermitidos(
        NombreEstadoOrden origen
    ) {
        Usuario administrador = usuario(40L, NombreRol.ADMINISTRADOR);
        OrdenTrabajo orden = orden(7L, origen, null);
        when(ordenTrabajoRepository.findById(7L))
            .thenReturn(Optional.of(orden));
        when(usuarioService.buscarEntidadPorId(40L))
            .thenReturn(administrador);
        when(estadoOrdenRepository.findByNombre(NombreEstadoOrden.CANCELADA))
            .thenReturn(Optional.of(estado(NombreEstadoOrden.CANCELADA)));

        var response = ordenTrabajoService.cancelar(
            7L,
            " Cliente desiste ",
            40L
        );

        assertThat(response.estado()).isEqualTo(NombreEstadoOrden.CANCELADA);
        assertThat(response.motivoCancelacion())
            .isEqualTo("Cliente desiste");
        HistorialOrden historial = historialGuardado();
        assertThat(historial.getTipoEvento())
            .isEqualTo(TipoEventoOrden.CANCELACION);
        assertThat(historial.getComentario()).isEqualTo("Cliente desiste");
        assertThat(historial.isVisibleCliente()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
        value = NombreEstadoOrden.class,
        names = {
            "EN_REPARACION",
            "FINALIZADA",
            "LISTA_PARA_RETIRAR",
            "ENTREGADA",
            "CANCELADA"
        }
    )
    void noDebeCancelarDesdeReparacionOEstadosPosteriores(
        NombreEstadoOrden estado
    ) {
        OrdenTrabajo orden = orden(7L, estado, null);
        when(ordenTrabajoRepository.findById(7L))
            .thenReturn(Optional.of(orden));

        assertThatThrownBy(() -> ordenTrabajoService.cancelar(
            7L,
            "Motivo",
            40L
        )).isInstanceOf(OrdenNoCancelableException.class);
    }

    private void prepararCambioEstado(
        OrdenTrabajo orden,
        Usuario actor,
        NombreEstadoOrden destino
    ) {
        when(ordenTrabajoRepository.findById(7L))
            .thenReturn(Optional.of(orden));
        when(usuarioService.buscarEntidadPorId(actor.getId()))
            .thenReturn(actor);
        when(estadoOrdenRepository.findByNombre(destino))
            .thenReturn(Optional.of(estado(destino)));
    }

    private HistorialOrden historialGuardado() {
        ArgumentCaptor<HistorialOrden> captor =
            ArgumentCaptor.forClass(HistorialOrden.class);
        verify(historialOrdenRepository).save(captor.capture());
        return captor.getValue();
    }

    private OrdenTrabajo orden(
        Long id,
        NombreEstadoOrden estado,
        Usuario tecnico
    ) {
        OrdenTrabajo orden = new OrdenTrabajo(
            equipo(3L, usuario(10L, NombreRol.CLIENTE)),
            tecnico,
            estado(estado),
            "No enciende",
            LocalDateTime.now()
        );
        ReflectionTestUtils.setField(orden, "id", id);
        return orden;
    }

    private EstadoOrden estado(NombreEstadoOrden nombre) {
        EstadoOrden estado = new EstadoOrden(
            nombre,
            nombre == NombreEstadoOrden.ENTREGADA
                || nombre == NombreEstadoOrden.CANCELADA
        );
        ReflectionTestUtils.setField(estado, "id", (long) nombre.ordinal() + 1);
        return estado;
    }

    private Equipo equipo(Long id, Usuario propietario) {
        Cliente cliente = new Cliente(
            propietario,
            "Ana",
            "Pérez",
            null,
            null
        );
        ReflectionTestUtils.setField(cliente, "id", 2L);
        Equipo equipo = new Equipo(
            cliente,
            "Notebook",
            "Dell",
            "XPS",
            "ABC",
            "Sin golpes"
        );
        ReflectionTestUtils.setField(equipo, "id", id);
        return equipo;
    }

    private Usuario usuario(Long id, NombreRol rol) {
        Usuario usuario = new Usuario(
            "hash",
            "usuario" + id + "@fixhub.com",
            null
        );
        usuario.agregarRol(new Rol(rol));
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
