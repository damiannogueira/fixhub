package fixhub.presupuestos.service;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.service.UsuarioService;
import fixhub.clientes.entity.Cliente;
import fixhub.clientes.entity.Equipo;
import fixhub.inventario.entity.Repuesto;
import fixhub.inventario.repository.RepuestoRepository;
import fixhub.ordenes.entity.EstadoOrden;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.entity.OrdenTrabajo;
import fixhub.ordenes.service.OrdenTrabajoService;
import fixhub.presupuestos.dto.CrearPresupuestoRequest;
import fixhub.presupuestos.dto.DecisionPresupuestoRequest;
import fixhub.presupuestos.dto.DetallePresupuestoRequest;
import fixhub.presupuestos.dto.PresupuestoResponse;
import fixhub.presupuestos.entity.EstadoPresupuesto;
import fixhub.presupuestos.entity.Presupuesto;
import fixhub.presupuestos.repository.PresupuestoRepository;
import fixhub.presupuestos.exception.PresupuestoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresupuestoServiceTest {

    @Mock
    private PresupuestoRepository presupuestoRepository;

    @Mock
    private RepuestoRepository repuestoRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private OrdenTrabajoService ordenTrabajoService;

    @InjectMocks
    private PresupuestoService presupuestoService;

    @Test
    void debeEditarPublicadoPendienteSinRepublicarlo() {
        Usuario recepcionista = usuario(20L, NombreRol.RECEPCIONISTA);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.PENDIENTE_APROBACION, null);
        Presupuesto publicado = presupuesto(1L, orden, EstadoPresupuesto.PUBLICADO);
        LocalDateTime fechaPublicacion = publicado.getFechaPublicacion();
        Usuario publicadoPor = publicado.getPublicadoPor();
        when(presupuestoRepository.findById(1L)).thenReturn(Optional.of(publicado));
        when(usuarioService.buscarEntidadPorId(20L)).thenReturn(recepcionista);
        when(presupuestoRepository.save(publicado)).thenReturn(publicado);

        PresupuestoResponse respuesta = presupuestoService.editar(1L,
            new CrearPresupuestoRequest(new BigDecimal("25.00"), List.of()), 20L);

        assertThat(respuesta.estado()).isEqualTo(EstadoPresupuesto.PUBLICADO);
        assertThat(respuesta.total()).isEqualByComparingTo("25.00");
        assertThat(publicado.getFechaPublicacion()).isEqualTo(fechaPublicacion);
        assertThat(publicado.getPublicadoPor()).isSameAs(publicadoPor);
        assertThat(publicado.getFechaDecision()).isNull();
        verify(ordenTrabajoService, never()).registrarPublicacionPresupuesto(anyLong(), anyLong());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPresupuesto.class, names = {"APROBADO", "RECHAZADO"})
    void noDebeEditarPresupuestoCerrado(EstadoPresupuesto estado) {
        Usuario recepcionista = usuario(20L, NombreRol.RECEPCIONISTA);
        when(presupuestoRepository.findById(1L)).thenReturn(Optional.of(
            presupuesto(1L, orden(7L, NombreEstadoOrden.PENDIENTE_APROBACION, null), estado)));
        when(usuarioService.buscarEntidadPorId(20L)).thenReturn(recepcionista);

        assertThatThrownBy(() -> presupuestoService.editar(1L,
            new CrearPresupuestoRequest(BigDecimal.TEN, List.of()), 20L))
            .isInstanceOf(IllegalStateException.class);
        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void noDebeEditarPublicadoSiLaOrdenYaNoEstaPendiente() {
        when(presupuestoRepository.findById(1L)).thenReturn(Optional.of(
            presupuesto(1L, orden(7L, NombreEstadoOrden.CANCELADA, null), EstadoPresupuesto.PUBLICADO)));
        when(usuarioService.buscarEntidadPorId(20L)).thenReturn(usuario(20L, NombreRol.RECEPCIONISTA));

        assertThatThrownBy(() -> presupuestoService.editar(1L,
            new CrearPresupuestoRequest(BigDecimal.TEN, List.of()), 20L))
            .isInstanceOf(IllegalStateException.class);
        verify(presupuestoRepository, never()).save(any());
    }

    // ---------- Visibilidad para el cliente ----------

    @Test
    void clienteNoDebeVerBorradoresEnSuListado() {
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.EN_DIAGNOSTICO, null);
        Presupuesto borrador = presupuesto(1L, orden, EstadoPresupuesto.BORRADOR);
        Presupuesto publicado = presupuesto(2L, orden, EstadoPresupuesto.PUBLICADO);
        Presupuesto aprobado = presupuesto(3L, orden, EstadoPresupuesto.APROBADO);
        when(presupuestoRepository
            .findAllByOrdenEquipoClienteUsuarioIdOrderByFechaCreacionDesc(10L))
            .thenReturn(List.of(borrador, publicado, aprobado));

        List<PresupuestoResponse> resultado = presupuestoService.listarPropios(10L);

        assertThat(resultado)
            .extracting(PresupuestoResponse::estado)
            .containsExactly(EstadoPresupuesto.PUBLICADO, EstadoPresupuesto.APROBADO);
    }

    @Test
    void clienteNoDebeConsultarUnBorradorPorOrden() {
        Usuario cliente = usuario(10L, NombreRol.CLIENTE);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.EN_DIAGNOSTICO, null);
        when(presupuestoRepository.findByOrdenId(7L))
            .thenReturn(Optional.of(presupuesto(1L, orden, EstadoPresupuesto.BORRADOR)));
        when(usuarioService.buscarEntidadPorId(10L)).thenReturn(cliente);

        assertThatThrownBy(() -> presupuestoService.buscarPorOrden(7L, 10L))
            .isInstanceOf(PresupuestoNoEncontradoException.class);
    }

    @Test
    void clienteDebeConsultarUnPresupuestoPublicado() {
        Usuario cliente = usuario(10L, NombreRol.CLIENTE);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.PENDIENTE_APROBACION, null);
        when(presupuestoRepository.findByOrdenId(7L))
            .thenReturn(Optional.of(presupuesto(1L, orden, EstadoPresupuesto.PUBLICADO)));
        when(usuarioService.buscarEntidadPorId(10L)).thenReturn(cliente);

        PresupuestoResponse respuesta = presupuestoService.buscarPorOrden(7L, 10L);

        assertThat(respuesta.estado()).isEqualTo(EstadoPresupuesto.PUBLICADO);
    }

    @Test
    void recepcionDebeConsultarUnBorrador() {
        Usuario recepcionista = usuario(20L, NombreRol.RECEPCIONISTA);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.EN_DIAGNOSTICO, null);
        when(presupuestoRepository.findByOrdenId(7L))
            .thenReturn(Optional.of(presupuesto(1L, orden, EstadoPresupuesto.BORRADOR)));
        when(usuarioService.buscarEntidadPorId(20L)).thenReturn(recepcionista);

        assertThat(presupuestoService.buscarPorOrden(7L, 20L).estado())
            .isEqualTo(EstadoPresupuesto.BORRADOR);
    }

    // ---------- Publicación ----------

    @Test
    void tecnicoNoDebePublicarPresupuestos() {
        Usuario tecnico = usuario(30L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.EN_DIAGNOSTICO, tecnico);
        when(presupuestoRepository.findById(1L))
            .thenReturn(Optional.of(presupuesto(1L, orden, EstadoPresupuesto.BORRADOR)));
        when(usuarioService.buscarEntidadPorId(30L)).thenReturn(tecnico);

        assertThatThrownBy(() -> presupuestoService.publicar(1L, 30L))
            .isInstanceOf(AccessDeniedException.class);
        verify(ordenTrabajoService, never()).registrarPublicacionPresupuesto(anyLong(), anyLong());
    }

    @Test
    void recepcionDebePublicarYPasarLaOrdenAPendienteDeAprobacion() {
        Usuario recepcionista = usuario(20L, NombreRol.RECEPCIONISTA);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.EN_DIAGNOSTICO, null);
        Presupuesto borrador = presupuesto(1L, orden, EstadoPresupuesto.BORRADOR);
        when(presupuestoRepository.findById(1L)).thenReturn(Optional.of(borrador));
        when(usuarioService.buscarEntidadPorId(20L)).thenReturn(recepcionista);
        when(presupuestoRepository.save(any(Presupuesto.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

        PresupuestoResponse respuesta = presupuestoService.publicar(1L, 20L);

        assertThat(respuesta.estado()).isEqualTo(EstadoPresupuesto.PUBLICADO);
        verify(ordenTrabajoService).registrarPublicacionPresupuesto(7L, 20L);
    }

    @Test
    void noDebePublicarSiLaOrdenYaNoEstaEnDiagnostico() {
        Usuario recepcionista = usuario(20L, NombreRol.RECEPCIONISTA);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.EN_REPARACION, null);
        when(presupuestoRepository.findById(1L))
            .thenReturn(Optional.of(presupuesto(1L, orden, EstadoPresupuesto.BORRADOR)));
        when(usuarioService.buscarEntidadPorId(20L)).thenReturn(recepcionista);

        assertThatThrownBy(() -> presupuestoService.publicar(1L, 20L))
            .isInstanceOf(IllegalStateException.class);
        verify(ordenTrabajoService, never()).registrarPublicacionPresupuesto(anyLong(), anyLong());
    }

    // ---------- Alta y edición ----------

    @Test
    void noDebeCrearUnPresupuestoFueraDeDiagnostico() {
        Usuario administrador = usuario(40L, NombreRol.ADMINISTRADOR);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.EN_REPARACION, null);
        when(ordenTrabajoService.buscarEntidadPorId(7L)).thenReturn(orden);
        when(presupuestoRepository.findByOrdenId(7L)).thenReturn(Optional.empty());
        when(usuarioService.buscarEntidadPorId(40L)).thenReturn(administrador);

        assertThatThrownBy(() -> presupuestoService.crear(
            7L,
            new CrearPresupuestoRequest(BigDecimal.TEN, List.of()),
            40L
        )).isInstanceOf(IllegalStateException.class);
        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void noDebeEditarUnBorradorFueraDeDiagnostico() {
        Usuario administrador = usuario(40L, NombreRol.ADMINISTRADOR);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.CANCELADA, null);
        when(presupuestoRepository.findById(1L))
            .thenReturn(Optional.of(presupuesto(1L, orden, EstadoPresupuesto.BORRADOR)));
        when(usuarioService.buscarEntidadPorId(40L)).thenReturn(administrador);

        assertThatThrownBy(() -> presupuestoService.editar(
            1L,
            new CrearPresupuestoRequest(BigDecimal.TEN, List.of()),
            40L
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void noDebeRepetirUnRepuestoEnElPresupuesto() {
        Usuario administrador = usuario(40L, NombreRol.ADMINISTRADOR);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.EN_DIAGNOSTICO, null);
        Repuesto repuesto = new Repuesto("R-1", "Pantalla", null, 1, BigDecimal.ONE, BigDecimal.TEN);
        ReflectionTestUtils.setField(repuesto, "id", 5L);
        when(ordenTrabajoService.buscarEntidadPorId(7L)).thenReturn(orden);
        when(presupuestoRepository.findByOrdenId(7L)).thenReturn(Optional.empty());
        when(usuarioService.buscarEntidadPorId(40L)).thenReturn(administrador);
        when(repuestoRepository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(repuesto));

        assertThatThrownBy(() -> presupuestoService.crear(
            7L,
            new CrearPresupuestoRequest(
                BigDecimal.TEN,
                List.of(new DetallePresupuestoRequest(5L, 1), new DetallePresupuestoRequest(5L, 2))
            ),
            40L
        )).isInstanceOf(IllegalArgumentException.class);
        verify(presupuestoRepository, never()).save(any());
    }

    // ---------- Decisión del cliente ----------

    @Test
    void clienteDebeAprobarYLaOrdenPasaAAprobada() {
        Usuario cliente = usuario(10L, NombreRol.CLIENTE);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.PENDIENTE_APROBACION, null);
        when(presupuestoRepository.findById(1L))
            .thenReturn(Optional.of(presupuesto(1L, orden, EstadoPresupuesto.PUBLICADO)));
        when(usuarioService.buscarEntidadPorId(10L)).thenReturn(cliente);
        when(presupuestoRepository.save(any(Presupuesto.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

        PresupuestoResponse respuesta = presupuestoService.decidir(
            1L,
            new DecisionPresupuestoRequest(EstadoPresupuesto.APROBADO),
            10L
        );

        assertThat(respuesta.estado()).isEqualTo(EstadoPresupuesto.APROBADO);
        verify(ordenTrabajoService).registrarAprobacionPresupuesto(7L, 10L);
    }

    @Test
    void clienteDebeRechazarYLaOrdenSeCancela() {
        Usuario cliente = usuario(10L, NombreRol.CLIENTE);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.PENDIENTE_APROBACION, null);
        when(presupuestoRepository.findById(1L))
            .thenReturn(Optional.of(presupuesto(1L, orden, EstadoPresupuesto.PUBLICADO)));
        when(usuarioService.buscarEntidadPorId(10L)).thenReturn(cliente);
        when(presupuestoRepository.save(any(Presupuesto.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

        PresupuestoResponse respuesta = presupuestoService.decidir(
            1L,
            new DecisionPresupuestoRequest(EstadoPresupuesto.RECHAZADO),
            10L
        );

        assertThat(respuesta.estado()).isEqualTo(EstadoPresupuesto.RECHAZADO);
        verify(ordenTrabajoService).cancelar(anyLong(), anyString(), anyLong());
    }

    @Test
    void noDebeDecidirSiLaOrdenYaNoEstaPendienteDeAprobacion() {
        Usuario cliente = usuario(10L, NombreRol.CLIENTE);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.CANCELADA, null);
        when(presupuestoRepository.findById(1L))
            .thenReturn(Optional.of(presupuesto(1L, orden, EstadoPresupuesto.PUBLICADO)));
        when(usuarioService.buscarEntidadPorId(10L)).thenReturn(cliente);

        assertThatThrownBy(() -> presupuestoService.decidir(
            1L,
            new DecisionPresupuestoRequest(EstadoPresupuesto.APROBADO),
            10L
        )).isInstanceOf(IllegalStateException.class);
        verify(ordenTrabajoService, never()).registrarAprobacionPresupuesto(anyLong(), anyLong());
    }

    @Test
    void clienteAjenoNoDebeDecidir() {
        Usuario ajeno = usuario(99L, NombreRol.CLIENTE);
        OrdenTrabajo orden = orden(7L, NombreEstadoOrden.PENDIENTE_APROBACION, null);
        when(presupuestoRepository.findById(1L))
            .thenReturn(Optional.of(presupuesto(1L, orden, EstadoPresupuesto.PUBLICADO)));
        when(usuarioService.buscarEntidadPorId(99L)).thenReturn(ajeno);

        assertThatThrownBy(() -> presupuestoService.decidir(
            1L,
            new DecisionPresupuestoRequest(EstadoPresupuesto.APROBADO),
            99L
        )).isInstanceOf(AccessDeniedException.class);
    }

    // ---------- Helpers ----------

    private Presupuesto presupuesto(Long id, OrdenTrabajo orden, EstadoPresupuesto estado) {
        Usuario recepcionista = usuario(20L, NombreRol.RECEPCIONISTA);
        Presupuesto presupuesto = new Presupuesto(orden, recepcionista, BigDecimal.TEN);
        ReflectionTestUtils.setField(presupuesto, "id", id);
        if (estado == EstadoPresupuesto.PUBLICADO) {
            presupuesto.publicar(recepcionista);
        } else if (estado != EstadoPresupuesto.BORRADOR) {
            presupuesto.decidir(usuario(10L, NombreRol.CLIENTE), estado);
        }
        return presupuesto;
    }

    private OrdenTrabajo orden(Long id, NombreEstadoOrden estado, Usuario tecnico) {
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
            nombre == NombreEstadoOrden.ENTREGADA || nombre == NombreEstadoOrden.CANCELADA
        );
        ReflectionTestUtils.setField(estado, "id", (long) nombre.ordinal() + 1);
        return estado;
    }

    private Equipo equipo(Long id, Usuario propietario) {
        Cliente cliente = new Cliente(propietario, "Ana", "Pérez", null, null);
        ReflectionTestUtils.setField(cliente, "id", 2L);
        Equipo equipo = new Equipo(cliente, "Notebook", "Dell", "XPS", "ABC", "Sin golpes");
        ReflectionTestUtils.setField(equipo, "id", id);
        return equipo;
    }

    private Usuario usuario(Long id, NombreRol rol) {
        Usuario usuario = new Usuario("hash", "usuario" + id + "@fixhub.com", null);
        usuario.agregarRol(new Rol(rol));
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
