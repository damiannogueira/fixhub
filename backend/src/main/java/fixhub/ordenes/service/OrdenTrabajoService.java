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
import fixhub.ordenes.exception.TecnicoRequeridoException;
import fixhub.ordenes.exception.TransicionEstadoInvalidaException;
import fixhub.ordenes.repository.EstadoOrdenRepository;
import fixhub.ordenes.repository.HistorialOrdenRepository;
import fixhub.ordenes.repository.OrdenTrabajoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
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

    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final EstadoOrdenRepository estadoOrdenRepository;
    private final HistorialOrdenRepository historialOrdenRepository;
    private final EquipoService equipoService;
    private final UsuarioService usuarioService;

    public OrdenTrabajoService(
        OrdenTrabajoRepository ordenTrabajoRepository,
        EstadoOrdenRepository estadoOrdenRepository,
        HistorialOrdenRepository historialOrdenRepository,
        EquipoService equipoService,
        UsuarioService usuarioService
    ) {
        this.ordenTrabajoRepository = ordenTrabajoRepository;
        this.estadoOrdenRepository = estadoOrdenRepository;
        this.historialOrdenRepository = historialOrdenRepository;
        this.equipoService = equipoService;
        this.usuarioService = usuarioService;
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

    @Transactional
    public OrdenTrabajoResponse asignarTecnico(
        Long ordenId,
        Long tecnicoId,
        Long actorId
    ) {
        OrdenTrabajo orden = buscarEntidadPorId(ordenId);
        if (orden.getEstado().isEstadoFinal()) {
            throw new TransicionEstadoInvalidaException(
                orden.getEstado().getNombre(),
                orden.getEstado().getNombre()
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
        LocalDateTime ahora = LocalDateTime.now();

        orden.setTecnico(tecnicoNuevo);
        ordenTrabajoRepository.save(orden);

        TipoEventoOrden evento = tecnicoAnterior == null
            ? TipoEventoOrden.ASIGNACION_TECNICO
            : TipoEventoOrden.CAMBIO_TECNICO;
        String comentario = tecnicoAnterior == null
            ? "Técnico asignado: " + tecnicoNuevo.getId()
            : "Técnico anterior: " + tecnicoAnterior.getId()
                + "; técnico nuevo: " + tecnicoNuevo.getId();

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
        EstadoOrden anterior = orden.getEstado();

        // Solo se permiten transiciones expresas.
        // Las relacionadas con presupuestos se habilitarán en su módulo.
        validarTransicion(orden, destino, actor);

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

    private OrdenTrabajo buscarEntidadPorId(Long id) {
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

        if (!actor.tieneRol(NombreRol.RECEPCIONISTA)) {
            throw new AccessDeniedException(
                "La transición debe realizarla un Recepcionista"
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
