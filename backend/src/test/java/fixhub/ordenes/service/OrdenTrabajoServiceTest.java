package fixhub.ordenes.service;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.service.UsuarioService;
import fixhub.clientes.entity.Cliente;
import fixhub.clientes.entity.Equipo;
import fixhub.clientes.service.EquipoService;
import fixhub.ordenes.dto.CrearOrdenTrabajoRequest;
import fixhub.ordenes.dto.HistorialOrdenResponse;
import fixhub.ordenes.exception.OrdenTrabajoNoEncontradaException;
import fixhub.ordenes.entity.EstadoOrden;
import fixhub.ordenes.entity.HistorialOrden;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.entity.OrdenTrabajo;
import fixhub.ordenes.entity.TipoEventoOrden;
import fixhub.ordenes.exception.OrdenNoCancelableException;
import fixhub.ordenes.exception.PresupuestoNoAprobadoException;
import fixhub.ordenes.exception.TecnicoRequeridoException;
import fixhub.ordenes.exception.TransicionEstadoInvalidaException;
import fixhub.ordenes.repository.EstadoOrdenRepository;
import fixhub.ordenes.repository.HistorialOrdenRepository;
import fixhub.ordenes.repository.OrdenTrabajoRepository;
import fixhub.presupuestos.entity.EstadoPresupuesto;
import fixhub.presupuestos.entity.Presupuesto;
import fixhub.presupuestos.repository.PresupuestoRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    @Mock
    private PresupuestoRepository presupuestoRepository;

    private OrdenTrabajoService ordenTrabajoService;

    @BeforeEach
    void configurar() {
        ordenTrabajoService = new OrdenTrabajoService(
            ordenTrabajoRepository,
            estadoOrdenRepository,
            historialOrdenRepository,
            equipoService,
            usuarioService,
            presupuestoRepository
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
    void administradorDebeCambiarTecnicoConMotivoEnElHistorialInterno() {
        Usuario actor = usuario(20L, NombreRol.ADMINISTRADOR);
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

        var response = ordenTrabajoService.asignarTecnico(
            7L,
            31L,
            "  El técnico original está de licencia  ",
            20L
        );

        assertThat(response.tecnicoId()).isEqualTo(31L);
        HistorialOrden historial = historialGuardado();
        assertThat(historial.getTipoEvento())
            .isEqualTo(TipoEventoOrden.CAMBIO_TECNICO);
        assertThat(historial.getComentario())
            .contains("30", "31")
            .endsWith("Motivo: El técnico original está de licencia");
        assertThat(historial.isVisibleCliente()).isFalse();
    }

    @Test
    void recepcionistaNoDebeCambiarUnTecnicoYaAsignado() {
        Usuario recepcionista = usuario(20L, NombreRol.RECEPCIONISTA);
        Usuario anterior = usuario(30L, NombreRol.TECNICO);
        Usuario nuevo = usuario(31L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.RECIBIDA, anterior);
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(orden));
        when(usuarioService.buscarTecnicoPorId(31L)).thenReturn(nuevo);
        when(usuarioService.buscarEntidadPorId(20L)).thenReturn(recepcionista);

        assertThatThrownBy(() -> ordenTrabajoService.asignarTecnico(
            7L,
            31L,
            "Motivo válido",
            20L
        )).isInstanceOf(AccessDeniedException.class);

        assertThat(orden.getTecnico()).isSameAs(anterior);
        verify(ordenTrabajoRepository, never()).save(any());
        verify(historialOrdenRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void cambiarUnTecnicoYaAsignadoExigeMotivo(String motivo) {
        Usuario administrador = usuario(40L, NombreRol.ADMINISTRADOR);
        Usuario anterior = usuario(30L, NombreRol.TECNICO);
        Usuario nuevo = usuario(31L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.EN_DIAGNOSTICO, anterior);
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(orden));
        when(usuarioService.buscarTecnicoPorId(31L)).thenReturn(nuevo);
        when(usuarioService.buscarEntidadPorId(40L)).thenReturn(administrador);

        assertThatThrownBy(() -> ordenTrabajoService.asignarTecnico(7L, 31L, motivo, 40L))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(orden.getTecnico()).isSameAs(anterior);
        verify(historialOrdenRepository, never()).save(any());
    }

    @Test
    void cambiarUnTecnicoYaAsignadoSinMotivoNuloTambienFalla() {
        Usuario administrador = usuario(40L, NombreRol.ADMINISTRADOR);
        Usuario anterior = usuario(30L, NombreRol.TECNICO);
        Usuario nuevo = usuario(31L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.RECIBIDA, anterior);
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(orden));
        when(usuarioService.buscarTecnicoPorId(31L)).thenReturn(nuevo);
        when(usuarioService.buscarEntidadPorId(40L)).thenReturn(administrador);

        assertThatThrownBy(() -> ordenTrabajoService.asignarTecnico(7L, 31L, null, 40L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(
        value = NombreEstadoOrden.class,
        names = {"FINALIZADA", "LISTA_PARA_RETIRAR", "ENTREGADA", "CANCELADA"}
    )
    void noDebeCambiarElTecnicoUnaVezFinalizadaLaReparacion(NombreEstadoOrden estado) {
        Usuario anterior = usuario(30L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(7L, estado, anterior);
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(orden));

        assertThatThrownBy(() -> ordenTrabajoService.asignarTecnico(7L, 31L, "Motivo", 40L))
            .isInstanceOf(IllegalStateException.class);
        verify(ordenTrabajoRepository, never()).save(any());
    }

    @Test
    void administradorDebeAsignarElPrimerTecnicoSinMotivo() {
        Usuario administrador = usuario(40L, NombreRol.ADMINISTRADOR);
        Usuario tecnico = usuario(30L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.RECIBIDA, null);
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(orden));
        when(usuarioService.buscarTecnicoPorId(30L)).thenReturn(tecnico);
        when(usuarioService.buscarEntidadPorId(40L)).thenReturn(administrador);

        var response = ordenTrabajoService.asignarTecnico(7L, 30L, null, 40L);

        assertThat(response.tecnicoId()).isEqualTo(30L);
        assertThat(historialGuardado().getTipoEvento())
            .isEqualTo(TipoEventoOrden.ASIGNACION_TECNICO);
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

        ordenTrabajoService.asignarTecnico(7L, 30L, null, 20L);

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

        ordenTrabajoService.asignarTecnico(7L, 30L, null, 20L);

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
        if (origen == NombreEstadoOrden.APROBADA) {
            when(presupuestoRepository.findByOrdenId(7L))
                .thenReturn(Optional.of(presupuesto(orden, EstadoPresupuesto.APROBADO)));
        }

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

    @ParameterizedTest
    @EnumSource(
        value = EstadoPresupuesto.class,
        names = {"BORRADOR", "PUBLICADO"}
    )
    void noDebeIniciarReparacionDesdeDiagnosticoSiHayPresupuesto(
        EstadoPresupuesto estadoPresupuesto
    ) {
        Usuario tecnico = usuario(30L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.EN_DIAGNOSTICO, tecnico);
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(orden));
        when(usuarioService.buscarEntidadPorId(30L)).thenReturn(tecnico);
        when(presupuestoRepository.findByOrdenId(7L))
            .thenReturn(Optional.of(presupuesto(orden, estadoPresupuesto)));

        assertThatThrownBy(() -> ordenTrabajoService.cambiarEstado(
            7L,
            NombreEstadoOrden.EN_REPARACION,
            30L
        )).isInstanceOf(PresupuestoNoAprobadoException.class);
        verify(historialOrdenRepository, never()).save(any());
    }

    @Test
    void noDebeIniciarReparacionDesdeAprobadaSinPresupuestoAprobado() {
        Usuario tecnico = usuario(30L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.APROBADA, tecnico);
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(orden));
        when(usuarioService.buscarEntidadPorId(30L)).thenReturn(tecnico);
        when(presupuestoRepository.findByOrdenId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenTrabajoService.cambiarEstado(
            7L,
            NombreEstadoOrden.EN_REPARACION,
            30L
        )).isInstanceOf(PresupuestoNoAprobadoException.class);
    }

    @Test
    void administradorNoDebeRealizarTransicionesTecnicas() {
        Usuario tecnico = usuario(30L, NombreRol.TECNICO);
        Usuario administrador = usuario(40L, NombreRol.ADMINISTRADOR);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.EN_DIAGNOSTICO, tecnico);
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(orden));
        when(usuarioService.buscarEntidadPorId(40L)).thenReturn(administrador);

        assertThatThrownBy(() -> ordenTrabajoService.cambiarEstado(
            7L,
            NombreEstadoOrden.EN_REPARACION,
            40L
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void administradorNoDebeSaltearEstados() {
        Usuario administrador = usuario(40L, NombreRol.ADMINISTRADOR);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.RECIBIDA, null);
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(orden));
        when(usuarioService.buscarEntidadPorId(40L)).thenReturn(administrador);

        assertThatThrownBy(() -> ordenTrabajoService.cambiarEstado(
            7L,
            NombreEstadoOrden.ENTREGADA,
            40L
        )).isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void administradorDebeRealizarTransicionesDeRecepcion() {
        Usuario administrador = usuario(40L, NombreRol.ADMINISTRADOR);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.FINALIZADA, null);
        prepararCambioEstado(orden, administrador, NombreEstadoOrden.LISTA_PARA_RETIRAR);

        var response = ordenTrabajoService.cambiarEstado(
            7L,
            NombreEstadoOrden.LISTA_PARA_RETIRAR,
            40L
        );

        assertThat(response.estado()).isEqualTo(NombreEstadoOrden.LISTA_PARA_RETIRAR);
    }

    @Test
    void publicacionDePresupuestoDebePasarAPendienteAprobacion() {
        Usuario recepcionista = usuario(20L, NombreRol.RECEPCIONISTA);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.EN_DIAGNOSTICO, null);
        prepararCambioEstado(orden, recepcionista, NombreEstadoOrden.PENDIENTE_APROBACION);

        var response = ordenTrabajoService.registrarPublicacionPresupuesto(7L, 20L);

        assertThat(response.estado()).isEqualTo(NombreEstadoOrden.PENDIENTE_APROBACION);
    }

    @ParameterizedTest
    @EnumSource(
        value = NombreEstadoOrden.class,
        names = {"EN_DIAGNOSTICO"},
        mode = EnumSource.Mode.EXCLUDE
    )
    void publicacionDePresupuestoSoloDesdeDiagnostico(NombreEstadoOrden origen) {
        OrdenTrabajo orden = orden(7L, origen, null);
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(orden));

        assertThatThrownBy(() -> ordenTrabajoService.registrarPublicacionPresupuesto(7L, 20L))
            .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void aprobacionDePresupuestoSoloDesdePendienteAprobacion() {
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.EN_REPARACION, null);
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(orden));

        assertThatThrownBy(() -> ordenTrabajoService.registrarAprobacionPresupuesto(7L, 10L))
            .isInstanceOf(TransicionEstadoInvalidaException.class);
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

    @Test
    void debeListarElHistorialDeLaOrdenConEstadosYResponsable() {
        Usuario recepcionista = usuario(20L, NombreRol.RECEPCIONISTA);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.EN_DIAGNOSTICO, null);
        HistorialOrden evento = new HistorialOrden(
            orden,
            recepcionista,
            TipoEventoOrden.CAMBIO_ESTADO,
            estado(NombreEstadoOrden.RECIBIDA),
            estado(NombreEstadoOrden.EN_DIAGNOSTICO),
            null,
            true,
            LocalDateTime.of(2026, 11, 9, 10, 0)
        );
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(orden));
        when(historialOrdenRepository.findAllByOrdenIdOrderByFechaAscIdAsc(7L))
            .thenReturn(List.of(evento));

        List<HistorialOrdenResponse> historial = ordenTrabajoService.listarHistorial(7L);

        assertThat(historial).hasSize(1);
        HistorialOrdenResponse respuesta = historial.get(0);
        assertThat(respuesta.tipoEvento()).isEqualTo(TipoEventoOrden.CAMBIO_ESTADO);
        assertThat(respuesta.estadoAnterior()).isEqualTo(NombreEstadoOrden.RECIBIDA);
        assertThat(respuesta.estadoNuevo()).isEqualTo(NombreEstadoOrden.EN_DIAGNOSTICO);
        assertThat(respuesta.usuarioNombre()).isEqualTo("usuario20");
    }

    @Test
    void historialDeUnaOrdenInexistenteDebeFallar() {
        when(ordenTrabajoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenTrabajoService.listarHistorial(99L))
            .isInstanceOf(OrdenTrabajoNoEncontradaException.class);
        verify(historialOrdenRepository, never()).findAllByOrdenIdOrderByFechaAscIdAsc(any());
    }

    @Test
    void debeListarLasReparacionesDeUnCliente() {
        OrdenTrabajo reciente = orden(8L, NombreEstadoOrden.EN_REPARACION, null);
        OrdenTrabajo anterior = orden(5L, NombreEstadoOrden.ENTREGADA, null);
        when(ordenTrabajoRepository.findAllByEquipoClienteIdOrderByFechaIngresoDescIdDesc(2L))
            .thenReturn(List.of(reciente, anterior));

        assertThat(ordenTrabajoService.listarPorCliente(2L))
            .extracting(respuesta -> respuesta.id())
            .containsExactly(8L, 5L);
    }

    @Test
    void debeListarLasReparacionesDeUnEquipoExistente() {
        OrdenTrabajo anterior = orden(5L, NombreEstadoOrden.ENTREGADA, null);
        when(ordenTrabajoRepository.findAllByEquipoIdOrderByFechaIngresoDescIdDesc(3L))
            .thenReturn(List.of(anterior));

        assertThat(ordenTrabajoService.listarPorEquipo(3L))
            .extracting(respuesta -> respuesta.id())
            .containsExactly(5L);
        verify(equipoService).buscarEntidadPorId(3L);
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

    private Presupuesto presupuesto(OrdenTrabajo orden, EstadoPresupuesto estado) {
        Usuario recepcionista = usuario(20L, NombreRol.RECEPCIONISTA);
        Presupuesto presupuesto = new Presupuesto(orden, recepcionista, BigDecimal.TEN);
        if (estado == EstadoPresupuesto.PUBLICADO) {
            presupuesto.publicar(recepcionista);
        } else if (estado != EstadoPresupuesto.BORRADOR) {
            presupuesto.decidir(usuario(10L, NombreRol.CLIENTE), estado);
        }
        return presupuesto;
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
