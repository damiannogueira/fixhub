package fixhub.inventario.service;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.service.UsuarioService;
import fixhub.inventario.dto.*;
import fixhub.inventario.entity.*;
import fixhub.inventario.exception.*;
import fixhub.inventario.repository.*;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.entity.OrdenTrabajo;
import fixhub.ordenes.service.OrdenTrabajoService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventarioService {
    private final RepuestoRepository repuestoRepository;
    private final MovimientoStockRepository movimientoRepository;
    private final ConsumoRepuestoRepository consumoRepository;
    private final UsuarioService usuarioService;
    private final OrdenTrabajoService ordenTrabajoService;

    public InventarioService(RepuestoRepository repuestoRepository,
                             MovimientoStockRepository movimientoRepository,
                             ConsumoRepuestoRepository consumoRepository,
                             UsuarioService usuarioService,
                             OrdenTrabajoService ordenTrabajoService) {
        this.repuestoRepository = repuestoRepository;
        this.movimientoRepository = movimientoRepository;
        this.consumoRepository = consumoRepository;
        this.usuarioService = usuarioService;
        this.ordenTrabajoService = ordenTrabajoService;
    }

    @Transactional
    public MovimientoStockResponse entrada(Long repuestoId, MovimientoStockRequest request, Long actorId) {
        Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        validarAdministrador(actor);
        Repuesto repuesto = repuestoForUpdate(repuestoId);
        repuesto.setStockActual(repuesto.getStockActual() + request.cantidad());
        MovimientoStock movimiento = movimientoRepository.save(new MovimientoStock(
            repuesto, actor, TipoMovimientoStock.ENTRADA, request.cantidad(),
            request.motivo().trim(), normalizar(request.referencia()), null, LocalDateTime.now()));
        return convertirMovimiento(movimiento);
    }

    @Transactional
    public MovimientoStockResponse ajuste(Long repuestoId, AjusteStockRequest request, Long actorId) {
        Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        validarAdministrador(actor);
        Repuesto repuesto = repuestoForUpdate(repuestoId);
        int nuevoStock = repuesto.getStockActual() + request.cantidad();
        if (nuevoStock < 0) {
            throw new StockInsuficienteException(repuesto.getNombre(), repuesto.getStockActual(), Math.abs(request.cantidad()));
        }
        repuesto.setStockActual(nuevoStock);
        MovimientoStock movimiento = movimientoRepository.save(new MovimientoStock(
            repuesto, actor, TipoMovimientoStock.AJUSTE, request.cantidad(),
            request.motivo().trim(), normalizar(request.referencia()), null, LocalDateTime.now()));
        return convertirMovimiento(movimiento);
    }

    @Transactional(readOnly = true)
    public List<MovimientoStockResponse> movimientos(Long repuestoId, Long actorId) {
        Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        if (!actor.tieneRol(NombreRol.ADMINISTRADOR)) {
            throw new AccessDeniedException("Solo el Administrador puede consultar movimientos de stock.");
        }
        repuestoRepository.findByIdAndActivoTrue(repuestoId)
            .orElseThrow(() -> new RepuestoNoEncontradoException(repuestoId));
        return movimientoRepository.findAllByRepuestoIdOrderByFechaDescIdDesc(repuestoId)
            .stream().map(this::convertirMovimiento).toList();
    }

    @Transactional
    public ConsumoRepuestoResponse consumir(Long ordenId, CrearConsumoRequest request, Long actorId) {
        Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        if (!actor.tieneRol(NombreRol.TECNICO)) {
            throw new AccessDeniedException("El consumo real solo puede registrarlo un Técnico.");
        }
        OrdenTrabajo orden = ordenTrabajoService.buscarEntidadPorId(ordenId);
        if (orden.getTecnico() == null || !orden.getTecnico().getId().equals(actorId)
            || orden.getEstado().getNombre() != NombreEstadoOrden.EN_REPARACION) {
            throw new ConsumoNoPermitidoException("El consumo solo puede registrarse en una orden asignada en reparación.");
        }
        Repuesto repuesto = repuestoForUpdate(request.repuestoId());
        if (repuesto.getStockActual() < request.cantidad()) {
            throw new StockInsuficienteException(repuesto.getNombre(), repuesto.getStockActual(), request.cantidad());
        }
        LocalDateTime ahora = LocalDateTime.now();
        repuesto.setStockActual(repuesto.getStockActual() - request.cantidad());
        ConsumoRepuesto consumo = consumoRepository.save(new ConsumoRepuesto(
            orden, repuesto, actor, request.cantidad(), repuesto.getPrecioVenta(), ahora));
        MovimientoStock movimiento = movimientoRepository.save(new MovimientoStock(
            repuesto, actor, TipoMovimientoStock.CONSUMO, -request.cantidad(),
            "Consumo real en orden #" + ordenId, "ORDEN-" + ordenId, consumo, ahora));
        return convertirConsumo(consumo);
    }

    @Transactional(readOnly = true)
    public List<ConsumoRepuestoResponse> consumosPorOrden(Long ordenId, Long actorId) {
        Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        OrdenTrabajo orden = ordenTrabajoService.buscarEntidadPorId(ordenId);
        if (!actor.tieneRol(NombreRol.ADMINISTRADOR)
            && !actor.tieneRol(NombreRol.RECEPCIONISTA)
            && !(actor.tieneRol(NombreRol.TECNICO) && orden.getTecnico() != null
                && orden.getTecnico().getId().equals(actorId))) {
            throw new AccessDeniedException("No puede consultar los consumos de esta orden.");
        }
        return consumoRepository.findAllByOrdenIdOrderByFechaAscIdAsc(ordenId)
            .stream().map(this::convertirConsumo).toList();
    }

    private Repuesto repuestoForUpdate(Long id) {
        return repuestoRepository.findActiveForUpdate(id)
            .orElseThrow(() -> new RepuestoNoEncontradoException(id));
    }

    private void validarAdministrador(Usuario actor) {
        if (!actor.tieneRol(NombreRol.ADMINISTRADOR)) {
            throw new AccessDeniedException("Solo el Administrador puede modificar el stock.");
        }
    }

    private String normalizar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private MovimientoStockResponse convertirMovimiento(MovimientoStock movimiento) {
        return new MovimientoStockResponse(movimiento.getId(), movimiento.getRepuesto().getId(),
            movimiento.getTipo(), movimiento.getCantidad(), movimiento.getMotivo(),
            movimiento.getReferencia(), movimiento.getUsuario().getId(),
            nombreUsuario(movimiento.getUsuario()),
            movimiento.getConsumo() == null ? null : movimiento.getConsumo().getId(), movimiento.getFecha());
    }

    private ConsumoRepuestoResponse convertirConsumo(ConsumoRepuesto consumo) {
        return new ConsumoRepuestoResponse(consumo.getId(), consumo.getOrden().getId(),
            consumo.getRepuesto().getId(), consumo.getRepuesto().getNombre(), consumo.getCantidad(),
            consumo.getPrecioUnitario(), consumo.getUsuario().getId(), nombreUsuario(consumo.getUsuario()), consumo.getFecha());
    }

    private String nombreUsuario(Usuario usuario) {
        if (usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
            return usuario.getEmail().split("@")[0];
        }
        return usuario.getTelefono();
    }
}
