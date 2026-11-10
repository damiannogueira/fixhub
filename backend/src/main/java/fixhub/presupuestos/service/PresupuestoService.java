package fixhub.presupuestos.service;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.service.UsuarioService;
import fixhub.inventario.entity.Repuesto;
import fixhub.inventario.repository.RepuestoRepository;
import fixhub.ordenes.entity.OrdenTrabajo;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.service.OrdenTrabajoService;
import fixhub.presupuestos.dto.*;
import fixhub.presupuestos.entity.*;
import fixhub.presupuestos.repository.PresupuestoRepository;
import fixhub.presupuestos.exception.PresupuestoNoEncontradoException;
import fixhub.inventario.exception.RepuestoNoEncontradoException;
import jakarta.persistence.EntityExistsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class PresupuestoService {
    private final PresupuestoRepository presupuestoRepository;
    private final RepuestoRepository repuestoRepository;
    private final UsuarioService usuarioService;
    private final OrdenTrabajoService ordenTrabajoService;

    public PresupuestoService(PresupuestoRepository presupuestoRepository, RepuestoRepository repuestoRepository,
                              UsuarioService usuarioService, OrdenTrabajoService ordenTrabajoService) {
        this.presupuestoRepository = presupuestoRepository; this.repuestoRepository = repuestoRepository;
        this.usuarioService = usuarioService; this.ordenTrabajoService = ordenTrabajoService;
    }

    @Transactional
    public PresupuestoResponse crear(Long ordenId, CrearPresupuestoRequest request, Long actorId) {
        OrdenTrabajo orden = ordenTrabajoService.buscarEntidadPorId(ordenId);
        if (presupuestoRepository.findByOrdenId(ordenId).isPresent()) throw new EntityExistsException("La orden ya tiene un presupuesto.");
        Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        validarEditor(actor, orden);
        validarOrdenEnDiagnostico(orden, "crear");
        Presupuesto presupuesto = new Presupuesto(orden, actor, request.costoManoObra());
        reemplazarDetalles(presupuesto, request.detalles());
        return convertir(presupuestoRepository.save(presupuesto));
    }

    @Transactional(readOnly = true)
    public PresupuestoResponse buscarPorOrden(Long ordenId, Long actorId) {
        Presupuesto presupuesto = presupuestoRepository.findByOrdenId(ordenId).orElseThrow(PresupuestoNoEncontradoException::new);
        validarConsulta(presupuesto, actorId);
        return convertir(presupuesto);
    }

    @Transactional(readOnly = true)
    public List<PresupuestoResponse> listarPropios(Long usuarioId) {
        // El borrador es interno: el cliente solo ve presupuestos ya publicados o decididos.
        return presupuestoRepository.findAllByOrdenEquipoClienteUsuarioIdOrderByFechaCreacionDesc(usuarioId)
            .stream().filter(presupuesto -> presupuesto.getEstado() != EstadoPresupuesto.BORRADOR)
            .map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public List<PresupuestoResponse> listarTodos() {
        return presupuestoRepository.findAllByOrderByFechaCreacionDesc().stream()
            .map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public List<PresupuestoResponse> listarAsignados(Long tecnicoId) {
        return presupuestoRepository.findAllByOrdenTecnicoIdOrderByFechaCreacionDesc(tecnicoId).stream()
            .map(this::convertir).toList();
    }

    @Transactional
    public PresupuestoResponse editar(Long id, CrearPresupuestoRequest request, Long actorId) {
        Presupuesto presupuesto = buscar(id); Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        validarEditor(actor, presupuesto.getOrden());
        boolean borradorEnDiagnostico = presupuesto.getEstado() == EstadoPresupuesto.BORRADOR
            && presupuesto.getOrden().getEstado().getNombre() == NombreEstadoOrden.EN_DIAGNOSTICO;
        boolean publicadoPendiente = presupuesto.getEstado() == EstadoPresupuesto.PUBLICADO
            && presupuesto.getOrden().getEstado().getNombre() == NombreEstadoOrden.PENDIENTE_APROBACION;
        if (!borradorEnDiagnostico && !publicadoPendiente) {
            throw new IllegalStateException("Solo se puede editar un borrador en diagnóstico o un presupuesto publicado pendiente de decisión.");
        }
        presupuesto.setCostoManoObra(request.costoManoObra());
        reemplazarDetalles(presupuesto, request.detalles());
        return convertir(presupuestoRepository.save(presupuesto));
    }

    @Transactional
    public PresupuestoResponse publicar(Long id, Long actorId) {
        Presupuesto presupuesto = buscar(id); Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        // El Técnico participa en la elaboración, pero solo Administrador y Recepcionista publican.
        if (!actor.tieneRol(NombreRol.ADMINISTRADOR) && !actor.tieneRol(NombreRol.RECEPCIONISTA))
            throw new AccessDeniedException("Solo el Administrador o el Recepcionista pueden publicar presupuestos.");
        if (presupuesto.getEstado() != EstadoPresupuesto.BORRADOR) throw new IllegalStateException("Solo se puede publicar un presupuesto borrador.");
        validarOrdenEnDiagnostico(presupuesto.getOrden(), "publicar");
        presupuesto.publicar(actor);
        ordenTrabajoService.registrarPublicacionPresupuesto(presupuesto.getOrden().getId(), actorId);
        return convertir(presupuestoRepository.save(presupuesto));
    }

    @Transactional
    public PresupuestoResponse decidir(Long id, DecisionPresupuestoRequest request, Long usuarioId) {
        Presupuesto presupuesto = buscar(id); Usuario cliente = usuarioService.buscarEntidadPorId(usuarioId);
        if (!cliente.tieneRol(NombreRol.CLIENTE) || !presupuesto.getOrden().getEquipo().getCliente().getUsuario().getId().equals(usuarioId))
            throw new AccessDeniedException("El presupuesto no pertenece al cliente autenticado.");
        if (presupuesto.getEstado() != EstadoPresupuesto.PUBLICADO) throw new IllegalStateException("El presupuesto no está pendiente de decisión.");
        if (presupuesto.getOrden().getEstado().getNombre() != NombreEstadoOrden.PENDIENTE_APROBACION)
            throw new IllegalStateException("La orden ya no está esperando la aprobación del presupuesto.");
        if (request.decision() == EstadoPresupuesto.APROBADO) {
            ordenTrabajoService.registrarAprobacionPresupuesto(presupuesto.getOrden().getId(), usuarioId);
        } else if (request.decision() == EstadoPresupuesto.RECHAZADO) {
            ordenTrabajoService.cancelar(presupuesto.getOrden().getId(), "Presupuesto rechazado por el cliente", usuarioId);
        } else throw new IllegalArgumentException("La decisión debe ser APROBADO o RECHAZADO.");
        presupuesto.decidir(cliente, request.decision());
        return convertir(presupuestoRepository.save(presupuesto));
    }

    // La creación y publicación se realizan durante el diagnóstico.
    private void validarOrdenEnDiagnostico(OrdenTrabajo orden, String accion) {
        if (orden.getEstado().getNombre() != NombreEstadoOrden.EN_DIAGNOSTICO)
            throw new IllegalStateException("Solo se puede " + accion + " un presupuesto mientras la orden está en diagnóstico.");
    }

    private void validarEditor(Usuario actor, OrdenTrabajo orden) {
        if (!actor.tieneRol(NombreRol.ADMINISTRADOR) && !actor.tieneRol(NombreRol.RECEPCIONISTA)
            && !(actor.tieneRol(NombreRol.TECNICO) && orden.getTecnico() != null && orden.getTecnico().getId().equals(actor.getId())))
            throw new AccessDeniedException("No puede gestionar este presupuesto.");
    }
    private void validarConsulta(Presupuesto presupuesto, Long actorId) {
        Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        if (actor.tieneRol(NombreRol.ADMINISTRADOR) || actor.tieneRol(NombreRol.RECEPCIONISTA)) return;
        if (actor.tieneRol(NombreRol.TECNICO) && presupuesto.getOrden().getTecnico() != null && presupuesto.getOrden().getTecnico().getId().equals(actorId)) return;
        if (actor.tieneRol(NombreRol.CLIENTE) && presupuesto.getOrden().getEquipo().getCliente().getUsuario().getId().equals(actorId)) {
            // Para el cliente un borrador no existe todavía.
            if (presupuesto.getEstado() == EstadoPresupuesto.BORRADOR) throw new PresupuestoNoEncontradoException();
            return;
        }
        throw new AccessDeniedException("No puede consultar este presupuesto.");
    }
    private Presupuesto buscar(Long id) { return presupuestoRepository.findById(id).orElseThrow(PresupuestoNoEncontradoException::new); }
    private void reemplazarDetalles(Presupuesto presupuesto, List<DetallePresupuestoRequest> requests) {
        List<DetallePresupuesto> detalles = requests == null ? List.of() : requests.stream().map(request -> {
            Repuesto repuesto = repuestoRepository.findByIdAndActivoTrue(request.repuestoId()).orElseThrow(() -> new RepuestoNoEncontradoException(request.repuestoId()));
            return new DetallePresupuesto(presupuesto, repuesto, request.cantidad(), repuesto.getPrecioVenta());
        }).toList();
        if (detalles.stream().map(detalle -> detalle.getRepuesto().getId()).distinct().count() != detalles.size()) {
            throw new IllegalArgumentException("No se puede repetir un repuesto en el presupuesto.");
        }
        presupuesto.getDetalles().clear(); presupuesto.getDetalles().addAll(detalles);
        BigDecimal repuestos = detalles.stream().map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        presupuesto.setTotal(presupuesto.getCostoManoObra().add(repuestos));
    }
    private PresupuestoResponse convertir(Presupuesto p) {
        return new PresupuestoResponse(p.getId(), p.getOrden().getId(), p.getEstado(), p.getCostoManoObra(), p.getTotal(),
            p.getFechaCreacion(), p.getFechaPublicacion(), p.getFechaDecision(),
            p.getDetalles().stream().map(d -> new DetallePresupuestoResponse(d.getRepuesto().getId(), d.getRepuesto().getNombre(), d.getCantidad(), d.getPrecioUnitario())).toList());
    }
}
