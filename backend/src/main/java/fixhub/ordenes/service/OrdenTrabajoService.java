package fixhub.ordenes.service;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.service.UsuarioService;
import fixhub.clientes.entity.Cliente;
import fixhub.clientes.entity.Equipo;
import fixhub.clientes.service.EquipoService;
import fixhub.ordenes.dto.ClienteOrdenResponse;
import fixhub.ordenes.dto.CrearOrdenTrabajoRequest;
import fixhub.ordenes.dto.EquipoOrdenResponse;
import fixhub.ordenes.dto.HistorialOrdenResponse;
import fixhub.ordenes.dto.OrdenClienteResponse;
import fixhub.ordenes.dto.OrdenTrabajoResponse;
import fixhub.ordenes.entity.EstadoOrden;
import fixhub.ordenes.entity.HistorialOrden;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.entity.OrdenTrabajo;
import fixhub.ordenes.entity.TipoEventoOrden;
import fixhub.ordenes.exception.EstadoOrdenNoEncontradoException;
import fixhub.ordenes.exception.OrdenNoCancelableException;
import fixhub.ordenes.exception.OrdenTrabajoNoEncontradaException;
import fixhub.ordenes.exception.PresupuestoNoAprobadoException;
import fixhub.ordenes.exception.TecnicoRequeridoException;
import fixhub.ordenes.exception.TransicionEstadoInvalidaException;
import fixhub.ordenes.repository.EstadoOrdenRepository;
import fixhub.ordenes.repository.HistorialOrdenRepository;
import fixhub.ordenes.repository.OrdenTrabajoRepository;
import fixhub.presupuestos.entity.EstadoPresupuesto;
import fixhub.presupuestos.entity.Presupuesto;
import fixhub.presupuestos.repository.PresupuestoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class OrdenTrabajoService {

    private static final Set<NombreEstadoOrden> ESTADOS_CANCELABLES =
        EnumSet.of(
            NombreEstadoOrden.RECIBIDA,
            NombreEstadoOrden.EN_DIAGNOSTICO,
            NombreEstadoOrden.PENDIENTE_APROBACION,
            NombreEstadoOrden.APROBADA
        );

    private static final Set<NombreEstadoOrden> ESTADOS_SIN_CAMBIO_DE_TECNICO =
        EnumSet.of(
            NombreEstadoOrden.ENTREGADA,
            NombreEstadoOrden.CANCELADA
        );

    // Separa, en el comentario del historial, los técnicos involucrados del motivo del cambio.
    static final String SEPARADOR_MOTIVO = " | Motivo: ";

    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final EstadoOrdenRepository estadoOrdenRepository;
    private final HistorialOrdenRepository historialOrdenRepository;
    private final EquipoService equipoService;
    private final UsuarioService usuarioService;
    private final PresupuestoRepository presupuestoRepository;

    public OrdenTrabajoService(
        OrdenTrabajoRepository ordenTrabajoRepository,
        EstadoOrdenRepository estadoOrdenRepository,
        HistorialOrdenRepository historialOrdenRepository,
        EquipoService equipoService,
        UsuarioService usuarioService,
        PresupuestoRepository presupuestoRepository
    ) {
        this.ordenTrabajoRepository = ordenTrabajoRepository;
        this.estadoOrdenRepository = estadoOrdenRepository;
        this.historialOrdenRepository = historialOrdenRepository;
        this.equipoService = equipoService;
        this.usuarioService = usuarioService;
        this.presupuestoRepository = presupuestoRepository;
    }

    @Transactional
    public OrdenTrabajoResponse crear(
        CrearOrdenTrabajoRequest request,
        Long actorId
    ) {
        Equipo equipo = equipoService.buscarEntidadPorId(request.equipoId());
        Usuario tecnico = request.tecnicoId() == null
            ? null
            : usuarioService.buscarTecnicoPorId(request.tecnicoId());
        EstadoOrden recibida = buscarEstado(NombreEstadoOrden.RECIBIDA);
        LocalDateTime ahora = LocalDateTime.now();

        OrdenTrabajo orden = ordenTrabajoRepository.save(
            new OrdenTrabajo(
                equipo,
                tecnico,
                recibida,
                request.problemaInicial().trim(),
                ahora
            )
        );

        if (tecnico != null) {
            Usuario actor = usuarioService.buscarEntidadPorId(actorId);
            guardarHistorial(
                orden,
                actor,
                TipoEventoOrden.ASIGNACION_TECNICO,
                null,
                null,
                "Técnico asignado: " + tecnico.getId(),
                false,
                ahora
            );
        }

        return convertirAResponse(orden);
    }

    @Transactional(readOnly = true)
    public List<OrdenTrabajoResponse> listar() {
        return ordenTrabajoRepository
            .findAllByOrderByFechaIngresoDescIdDesc()
            .stream()
            .map(this::convertirAResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public OrdenTrabajoResponse buscarPorId(Long id) {
        return convertirAResponse(buscarEntidadPorId(id));
    }

    /** Reparaciones (actuales y anteriores) de un cliente, de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    public List<OrdenTrabajoResponse> listarPorCliente(Long clienteId) {
        return ordenTrabajoRepository
            .findAllByEquipoClienteIdOrderByFechaIngresoDescIdDesc(clienteId)
            .stream()
            .map(this::convertirAResponse)
            .toList();
    }

    /** Reparaciones (actuales y anteriores) de un equipo, de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    public List<OrdenTrabajoResponse> listarPorEquipo(Long equipoId) {
        equipoService.buscarEntidadPorId(equipoId);
        return ordenTrabajoRepository
            .findAllByEquipoIdOrderByFechaIngresoDescIdDesc(equipoId)
            .stream()
            .map(this::convertirAResponse)
            .toList();
    }

    /** Cambios relevantes de la orden, en orden cronológico. */
    @Transactional(readOnly = true)
    public List<HistorialOrdenResponse> listarHistorial(Long ordenId) {
        buscarEntidadPorId(ordenId);
        return historialOrdenRepository
            .findAllByOrdenIdOrderByFechaAscIdAsc(ordenId)
            .stream()
            .map(this::convertirHistorial)
            .toList();
    }

    private HistorialOrdenResponse convertirHistorial(HistorialOrden evento) {
        Usuario usuario = evento.getUsuario();
        String nombre = usuario.getEmail() != null && !usuario.getEmail().isBlank()
            ? usuario.getEmail().split("@")[0]
            : usuario.getTelefono();
        return new HistorialOrdenResponse(
            evento.getId(),
            evento.getTipoEvento(),
            evento.getEstadoAnterior() == null
                ? null
                : evento.getEstadoAnterior().getNombre(),
            evento.getEstadoNuevo() == null
                ? null
                : evento.getEstadoNuevo().getNombre(),
            evento.getComentario(),
            usuario.getId(),
            nombre,
            evento.isVisibleCliente(),
            evento.getFecha()
        );
    }

    /**
     * Asigna el técnico responsable de la orden.
     *
     * <p>El Administrador y el Recepcionista pueden asignar o cambiar al
     * responsable mientras la orden no sea terminal. El motivo es opcional
     * y el cambio queda registrado en el historial de la orden.
     */
    @Transactional
    public OrdenTrabajoResponse asignarTecnico(
        Long ordenId,
        Long tecnicoId,
        String motivo,
        Long actorId
    ) {
        OrdenTrabajo orden = buscarEntidadPorId(ordenId);
        if (ESTADOS_SIN_CAMBIO_DE_TECNICO.contains(orden.getEstado().getNombre())) {
            throw new IllegalStateException(
                "No se puede asignar ni cambiar el técnico de una orden en estado "
                    + orden.getEstado().getNombre()
            );
        }

        Usuario tecnicoAnterior = orden.getTecnico();
        Usuario tecnicoNuevo = usuarioService.buscarTecnicoPorId(tecnicoId);
        if (
            tecnicoAnterior != null
                && tecnicoAnterior.getId().equals(tecnicoNuevo.getId())
        ) {
            return convertirAResponse(orden);
        }

        Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        String motivoNormalizado = motivo == null ? "" : motivo.trim();
        if (tecnicoAnterior != null) {
            if (!actor.tieneRol(NombreRol.ADMINISTRADOR)
                && !actor.tieneRol(NombreRol.RECEPCIONISTA)) {
                throw new AccessDeniedException(
                    "Solo el Administrador o el Recepcionista pueden cambiar el técnico responsable"
                );
            }
        }
        LocalDateTime ahora = LocalDateTime.now();

        orden.setTecnico(tecnicoNuevo);
        ordenTrabajoRepository.save(orden);

        TipoEventoOrden evento = tecnicoAnterior == null
            ? TipoEventoOrden.ASIGNACION_TECNICO
            : TipoEventoOrden.CAMBIO_TECNICO;
        String comentario = tecnicoAnterior == null
            ? "Técnico asignado: " + tecnicoNuevo.getId()
            : "Técnico anterior: " + tecnicoAnterior.getId()
                + "; técnico nuevo: " + tecnicoNuevo.getId()
                + (motivoNormalizado.isEmpty() ? "" : SEPARADOR_MOTIVO + motivoNormalizado);

        guardarHistorial(
            orden,
            actor,
            evento,
            null,
            null,
            comentario,
            false,
            ahora
        );

        return convertirAResponse(orden);
    }

    @Transactional
    public OrdenTrabajoResponse cambiarEstado(
        Long ordenId,
        NombreEstadoOrden destino,
        Long actorId
    ) {
        OrdenTrabajo orden = buscarEntidadPorId(ordenId);
        Usuario actor = usuarioService.buscarEntidadPorId(actorId);

        // Solo se permiten transiciones expresas. Las que dependen del
        // presupuesto (PENDIENTE_APROBACION y APROBADA) no pasan por acá:
        // las dispara el módulo de presupuestos al publicar y al aprobar.
        validarTransicion(orden, destino, actor);
        validarPresupuestoParaReparacion(orden, destino);

        return aplicarCambioEstado(orden, destino, actor);
    }

    /** Publicación del presupuesto: EN_DIAGNOSTICO → PENDIENTE_APROBACION. */
    @Transactional
    public OrdenTrabajoResponse registrarPublicacionPresupuesto(
        Long ordenId,
        Long actorId
    ) {
        OrdenTrabajo orden = buscarEntidadPorId(ordenId);
        exigirEstado(
            orden,
            NombreEstadoOrden.EN_DIAGNOSTICO,
            NombreEstadoOrden.PENDIENTE_APROBACION
        );
        return aplicarCambioEstado(
            orden,
            NombreEstadoOrden.PENDIENTE_APROBACION,
            usuarioService.buscarEntidadPorId(actorId)
        );
    }

    /** Aprobación del cliente: PENDIENTE_APROBACION → APROBADA. */
    @Transactional
    public OrdenTrabajoResponse registrarAprobacionPresupuesto(
        Long ordenId,
        Long actorId
    ) {
        OrdenTrabajo orden = buscarEntidadPorId(ordenId);
        exigirEstado(
            orden,
            NombreEstadoOrden.PENDIENTE_APROBACION,
            NombreEstadoOrden.APROBADA
        );
        return aplicarCambioEstado(
            orden,
            NombreEstadoOrden.APROBADA,
            usuarioService.buscarEntidadPorId(actorId)
        );
    }

    private void exigirEstado(
        OrdenTrabajo orden,
        NombreEstadoOrden requerido,
        NombreEstadoOrden destino
    ) {
        NombreEstadoOrden actual = orden.getEstado().getNombre();
        if (actual != requerido) {
            throw new TransicionEstadoInvalidaException(actual, destino);
        }
    }

    // Si la orden tiene presupuesto, la reparación solo puede iniciarse
    // desde APROBADA y con el presupuesto aprobado por el cliente.
    private void validarPresupuestoParaReparacion(
        OrdenTrabajo orden,
        NombreEstadoOrden destino
    ) {
        if (destino != NombreEstadoOrden.EN_REPARACION) {
            return;
        }
        NombreEstadoOrden origen = orden.getEstado().getNombre();
        Optional<Presupuesto> presupuesto =
            presupuestoRepository.findByOrdenId(orden.getId());

        if (origen == NombreEstadoOrden.EN_DIAGNOSTICO
            && presupuesto.isPresent()) {
            throw new PresupuestoNoAprobadoException(
                "La orden tiene un presupuesto: la reparación solo puede "
                    + "iniciarse cuando el cliente lo apruebe"
            );
        }
        if (origen == NombreEstadoOrden.APROBADA
            && presupuesto
                .map(p -> p.getEstado() != EstadoPresupuesto.APROBADO)
                .orElse(true)) {
            throw new PresupuestoNoAprobadoException(
                "La reparación requiere un presupuesto aprobado por el cliente"
            );
        }
    }

    private OrdenTrabajoResponse aplicarCambioEstado(
        OrdenTrabajo orden,
        NombreEstadoOrden destino,
        Usuario actor
    ) {
        EstadoOrden anterior = orden.getEstado();
        EstadoOrden nuevo = buscarEstado(destino);
        LocalDateTime ahora = LocalDateTime.now();
        orden.setEstado(nuevo);
        if (destino == NombreEstadoOrden.ENTREGADA) {
            orden.setFechaEntrega(ahora);
        }
        ordenTrabajoRepository.save(orden);

        guardarHistorial(
            orden,
            actor,
            TipoEventoOrden.CAMBIO_ESTADO,
            anterior,
            nuevo,
            null,
            true,
            ahora
        );

        return convertirAResponse(orden);
    }

    @Transactional
    public OrdenTrabajoResponse cancelar(
        Long ordenId,
        String motivo,
        Long actorId
    ) {
        OrdenTrabajo orden = buscarEntidadPorId(ordenId);
        EstadoOrden anterior = orden.getEstado();
        if (!ESTADOS_CANCELABLES.contains(anterior.getNombre())) {
            throw new OrdenNoCancelableException(anterior.getNombre());
        }

        Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        EstadoOrden cancelada = buscarEstado(NombreEstadoOrden.CANCELADA);
        LocalDateTime ahora = LocalDateTime.now();
        String motivoNormalizado = motivo.trim();

        orden.setMotivoCancelacion(motivoNormalizado);
        orden.setEstado(cancelada);
        ordenTrabajoRepository.save(orden);

        guardarHistorial(
            orden,
            actor,
            TipoEventoOrden.CANCELACION,
            anterior,
            cancelada,
            motivoNormalizado,
            true,
            ahora
        );

        return convertirAResponse(orden);
    }

    @Transactional(readOnly = true)
    public List<OrdenTrabajoResponse> listarAsignadas(Long tecnicoId) {
        return ordenTrabajoRepository
            .findAllByTecnicoIdOrderByFechaIngresoDescIdDesc(tecnicoId)
            .stream()
            .map(this::convertirAResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public OrdenTrabajoResponse buscarAsignada(Long id, Long tecnicoId) {
        // La propiedad se comprueba en la consulta para no revelar órdenes ajenas.
        OrdenTrabajo orden = ordenTrabajoRepository
            .findByIdAndTecnicoId(id, tecnicoId)
            .orElseThrow(() -> new OrdenTrabajoNoEncontradaException(id));
        return convertirAResponse(orden);
    }

    @Transactional(readOnly = true)
    public List<OrdenClienteResponse> listarPropiasCliente(Long usuarioId) {
        return ordenTrabajoRepository
            .findAllByEquipoClienteUsuarioIdOrderByFechaIngresoDescIdDesc(
                usuarioId
            )
            .stream()
            .map(this::convertirAResponseCliente)
            .toList();
    }

    @Transactional(readOnly = true)
    public OrdenClienteResponse buscarPropiaCliente(
        Long id,
        Long usuarioId
    ) {
        // La ruta del cliente usa el usuario autenticado, no un clienteId externo.
        OrdenTrabajo orden = ordenTrabajoRepository
            .findByIdAndEquipoClienteUsuarioId(id, usuarioId)
            .orElseThrow(() -> new OrdenTrabajoNoEncontradaException(id));
        return convertirAResponseCliente(orden);
    }

    @Transactional(readOnly = true)
    public OrdenTrabajo buscarEntidadPorId(Long id) {
        return ordenTrabajoRepository.findById(id)
            .orElseThrow(() -> new OrdenTrabajoNoEncontradaException(id));
    }

    private EstadoOrden buscarEstado(NombreEstadoOrden nombre) {
        return estadoOrdenRepository.findByNombre(nombre)
            .orElseThrow(() -> new EstadoOrdenNoEncontradoException(nombre));
    }

    private void validarTransicion(
        OrdenTrabajo orden,
        NombreEstadoOrden destino,
        Usuario actor
    ) {
        NombreEstadoOrden origen = orden.getEstado().getNombre();
        boolean tecnica = esTransicionTecnica(origen, destino);
        boolean recepcion = esTransicionRecepcion(origen, destino);

        if (!tecnica && !recepcion) {
            throw new TransicionEstadoInvalidaException(origen, destino);
        }

        if (tecnica) {
            validarTecnicoAsignado(orden, actor);
            return;
        }

        // Las tareas técnicas quedan reservadas al Técnico asignado, incluso
        // para el Administrador; las de recepción y entrega las realiza
        // Recepción o Administración.
        if (!actor.tieneRol(NombreRol.RECEPCIONISTA)
            && !actor.tieneRol(NombreRol.ADMINISTRADOR)) {
            throw new AccessDeniedException(
                "La transición debe realizarla un Recepcionista o un Administrador"
            );
        }
    }

    // El paso directo de diagnóstico a reparación representa
    // el flujo sin presupuesto.
    private boolean esTransicionTecnica(
        NombreEstadoOrden origen,
        NombreEstadoOrden destino
    ) {
        return (origen == NombreEstadoOrden.RECIBIDA
                && destino == NombreEstadoOrden.EN_DIAGNOSTICO)
            || (origen == NombreEstadoOrden.EN_DIAGNOSTICO
                && destino == NombreEstadoOrden.EN_REPARACION)
            || (origen == NombreEstadoOrden.APROBADA
                && destino == NombreEstadoOrden.EN_REPARACION)
            || (origen == NombreEstadoOrden.EN_REPARACION
                && destino == NombreEstadoOrden.FINALIZADA);
    }

    private boolean esTransicionRecepcion(
        NombreEstadoOrden origen,
        NombreEstadoOrden destino
    ) {
        return (origen == NombreEstadoOrden.FINALIZADA
                && destino == NombreEstadoOrden.LISTA_PARA_RETIRAR)
            || (origen == NombreEstadoOrden.LISTA_PARA_RETIRAR
                && destino == NombreEstadoOrden.ENTREGADA);
    }

    private void validarTecnicoAsignado(
        OrdenTrabajo orden,
        Usuario actor
    ) {
        Usuario tecnico = orden.getTecnico();
        if (tecnico == null) {
            throw new TecnicoRequeridoException();
        }

        if (
            !actor.tieneRol(NombreRol.TECNICO)
                || !tecnico.getId().equals(actor.getId())
        ) {
            throw new AccessDeniedException(
                "La transición debe realizarla el Técnico asignado"
            );
        }
    }

    // El historial se guarda dentro de la misma transacción
    // que modifica la orden.
    private void guardarHistorial(
        OrdenTrabajo orden,
        Usuario actor,
        TipoEventoOrden tipo,
        EstadoOrden anterior,
        EstadoOrden nuevo,
        String comentario,
        boolean visibleCliente,
        LocalDateTime fecha
    ) {
        historialOrdenRepository.save(
            new HistorialOrden(
                orden,
                actor,
                tipo,
                anterior,
                nuevo,
                comentario,
                visibleCliente,
                fecha
            )
        );
    }

    private OrdenTrabajoResponse convertirAResponse(OrdenTrabajo orden) {
        Equipo equipo = orden.getEquipo();
        Cliente cliente = equipo.getCliente();
        Usuario tecnico = orden.getTecnico();

        return new OrdenTrabajoResponse(
            orden.getId(),
            convertirEquipo(equipo),
            new ClienteOrdenResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getApellido()
            ),
            tecnico == null ? null : tecnico.getId(),
            orden.getEstado().getNombre(),
            orden.getProblemaInicial(),
            orden.getMotivoCancelacion(),
            orden.getFechaIngreso(),
            orden.getFechaEntrega()
        );
    }

    private OrdenClienteResponse convertirAResponseCliente(
        OrdenTrabajo orden
    ) {
        return new OrdenClienteResponse(
            orden.getId(),
            convertirEquipo(orden.getEquipo()),
            orden.getEstado().getNombre(),
            orden.getProblemaInicial(),
            orden.getMotivoCancelacion(),
            orden.getFechaIngreso(),
            orden.getFechaEntrega()
        );
    }

    private EquipoOrdenResponse convertirEquipo(Equipo equipo) {
        return new EquipoOrdenResponse(
            equipo.getId(),
            equipo.getTipo(),
            equipo.getMarca(),
            equipo.getModelo(),
            equipo.getNumeroSerie(),
            equipo.getObservaciones()
        );
    }
}
