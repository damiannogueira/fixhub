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
        Presupuesto presupuesto = new Presupuesto(orden, actor, request.costoManoObra());
        reemplazarDetalles(presupuesto, request.detalles());
        return convertir(presupuestoRepository.save(presupuesto));
    }

    @Transactional(readOnly = true)
    public PresupuestoResponse buscarPorOrden(Long ordenId, Long actorId) {
        Presupuesto presupuesto = presupuestoRepository.findByOrdenId(ordenId).orElseThrow(() -> new IllegalArgumentException("La orden no tiene presupuesto."));
        validarConsulta(presupuesto, actorId);
        return convertir(presupuesto);
    }

    @Transactional(readOnly = true)
    public List<PresupuestoResponse> listarPropios(Long usuarioId) {
        return presupuestoRepository.findAllByOrdenEquipoClienteUsuarioIdOrderByFechaCreacionDesc(usuarioId)
            .stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public List<PresupuestoResponse> listarTodos() {
        return presupuestoRepository.findAllByOrderByFechaCreacionDesc().stream()
            .map(this::convertir).toList();
    }

    @Transactional
    public PresupuestoResponse editar(Long id, CrearPresupuestoRequest request, Long actorId) {
        Presupuesto presupuesto = buscar(id); Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        validarEditor(actor, presupuesto.getOrden());
        if (presupuesto.getEstado() != EstadoPresupuesto.BORRADOR) throw new IllegalStateException("Solo se puede editar un presupuesto borrador.");
        presupuesto.setCostoManoObra(request.costoManoObra());
        reemplazarDetalles(presupuesto, request.detalles());
        return convertir(presupuestoRepository.save(presupuesto));
    }

    @Transactional
    public PresupuestoResponse publicar(Long id, Long actorId) {
        Presupuesto presupuesto = buscar(id); Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        validarEditor(actor, presupuesto.getOrden());
        if (presupuesto.getEstado() != EstadoPresupuesto.BORRADOR) throw new IllegalStateException("Solo se puede publicar un presupuesto borrador.");
        presupuesto.publicar(actor);
        ordenTrabajoService.cambiarEstado(presupuesto.getOrden().getId(), NombreEstadoOrden.PENDIENTE_APROBACION, actorId);
        return convertir(presupuestoRepository.save(presupuesto));
    }

    @Transactional
    public PresupuestoResponse decidir(Long id, DecisionPresupuestoRequest request, Long usuarioId) {
        Presupuesto presupuesto = buscar(id); Usuario cliente = usuarioService.buscarEntidadPorId(usuarioId);
        if (!cliente.tieneRol(NombreRol.CLIENTE) || !presupuesto.getOrden().getEquipo().getCliente().getUsuario().getId().equals(usuarioId))
            throw new AccessDeniedException("El presupuesto no pertenece al cliente autenticado.");
        if (presupuesto.getEstado() != EstadoPresupuesto.PUBLICADO) throw new IllegalStateException("El presupuesto no está pendiente de decisión.");
        if (request.decision() == EstadoPresupuesto.APROBADO) {
            ordenTrabajoService.cambiarEstado(presupuesto.getOrden().getId(), NombreEstadoOrden.APROBADA, usuarioId);
        } else if (request.decision() == EstadoPresupuesto.RECHAZADO) {
            ordenTrabajoService.cancelar(presupuesto.getOrden().getId(), "Presupuesto rechazado por el cliente", usuarioId);
        } else throw new IllegalArgumentException("La decisión debe ser APROBADO o RECHAZADO.");
        presupuesto.decidir(cliente, request.decision());
        return convertir(presupuestoRepository.save(presupuesto));
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
        if (actor.tieneRol(NombreRol.CLIENTE) && presupuesto.getOrden().getEquipo().getCliente().getUsuario().getId().equals(actorId)) return;
        throw new AccessDeniedException("No puede consultar este presupuesto.");
    }
    private Presupuesto buscar(Long id) { return presupuestoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Presupuesto no encontrado.")); }
    private void reemplazarDetalles(Presupuesto presupuesto, List<DetallePresupuestoRequest> requests) {
        List<DetallePresupuesto> detalles = requests == null ? List.of() : requests.stream().map(request -> {
            Repuesto repuesto = repuestoRepository.findByIdAndActivoTrue(request.repuestoId()).orElseThrow(() -> new IllegalArgumentException("Repuesto no encontrado."));
            return new DetallePresupuesto(presupuesto, repuesto, request.cantidad(), repuesto.getPrecioVenta());
        }).toList();
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
