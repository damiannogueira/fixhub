package fixhub.inventario.service;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.service.UsuarioService;
import fixhub.inventario.dto.AjustarStockRequest;
import fixhub.inventario.dto.ConsumoRepuestoResponse;
import fixhub.inventario.dto.MovimientoStockResponse;
import fixhub.inventario.dto.RegistrarConsumoRepuestoRequest;
import fixhub.inventario.dto.RegistrarEntradaStockRequest;
import fixhub.inventario.entity.ConsumoRepuesto;
import fixhub.inventario.entity.MovimientoStock;
import fixhub.inventario.entity.Repuesto;
import fixhub.inventario.entity.TipoMovimientoStock;
import fixhub.inventario.exception.AjusteStockSinCambiosException;
import fixhub.inventario.exception.EstadoOrdenInvalidoParaConsumoException;
import fixhub.inventario.exception.RepuestoNoEncontradoException;
import fixhub.inventario.repository.ConsumoRepuestoRepository;
import fixhub.inventario.repository.MovimientoStockRepository;
import fixhub.inventario.repository.RepuestoRepository;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.entity.OrdenTrabajo;
import fixhub.ordenes.service.OrdenTrabajoService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockService {

    private final RepuestoRepository repuestoRepository;
    private final ConsumoRepuestoRepository consumoRepuestoRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final UsuarioService usuarioService;
    private final OrdenTrabajoService ordenTrabajoService;

    public StockService(
        RepuestoRepository repuestoRepository,
        ConsumoRepuestoRepository consumoRepuestoRepository,
        MovimientoStockRepository movimientoStockRepository,
        UsuarioService usuarioService,
        OrdenTrabajoService ordenTrabajoService
    ) {
        this.repuestoRepository = repuestoRepository;
        this.consumoRepuestoRepository =
            consumoRepuestoRepository;
        this.movimientoStockRepository =
            movimientoStockRepository;
        this.usuarioService = usuarioService;
        this.ordenTrabajoService = ordenTrabajoService;
    }

    @Transactional
    public MovimientoStockResponse registrarEntrada(
        Long repuestoId,
        RegistrarEntradaStockRequest request,
        Long actorId
    ) {
        Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        validarAdministrador(actor);

        Repuesto repuesto =
            buscarRepuestoActivoParaActualizar(repuestoId);

        repuesto.agregarStock(request.cantidad());
        repuestoRepository.save(repuesto);

        MovimientoStock movimiento =
            movimientoStockRepository.save(
                new MovimientoStock(
                    repuesto,
                    actor,
                    null,
                    TipoMovimientoStock.ENTRADA,
                    request.cantidad(),
                    request.motivo().trim(),
                    normalizarTextoOpcional(
                        request.referencia()
                    ),
                    LocalDateTime.now()
                )
            );

        return convertirMovimientoAResponse(movimiento);
    }

    @Transactional
    public MovimientoStockResponse ajustarStock(
        Long repuestoId,
        AjustarStockRequest request,
        Long actorId
    ) {
        Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        validarAdministrador(actor);

        Repuesto repuesto =
            buscarRepuestoActivoParaActualizar(repuestoId);

        Integer stockAnterior = repuesto.getStockActual();
        Integer diferencia =
            request.nuevoStock() - stockAnterior;

        if (diferencia == 0) {
            throw new AjusteStockSinCambiosException(repuestoId);
        }

        repuesto.ajustarStock(request.nuevoStock());
        repuestoRepository.save(repuesto);

        MovimientoStock movimiento =
            movimientoStockRepository.save(
                new MovimientoStock(
                    repuesto,
                    actor,
                    null,
                    TipoMovimientoStock.AJUSTE,
                    diferencia,
                    request.motivo().trim(),
                    normalizarTextoOpcional(
                        request.referencia()
                    ),
                    LocalDateTime.now()
                )
            );

        return convertirMovimientoAResponse(movimiento);
    }

    @Transactional
    public ConsumoRepuestoResponse registrarConsumo(
        Long ordenId,
        RegistrarConsumoRepuestoRequest request,
        Long actorId
    ) {
        Usuario actor = usuarioService.buscarEntidadPorId(actorId);

        if (!actor.tieneRol(NombreRol.TECNICO)) {
            throw new AccessDeniedException(
                "El consumo debe registrarlo un Técnico"
            );
        }

        OrdenTrabajo orden =
            ordenTrabajoService.buscarEntidadPorId(ordenId);

        validarTecnicoAsignado(orden, actor);
        validarEstadoParaConsumo(orden);

        Repuesto repuesto =
            buscarRepuestoActivoParaActualizar(
                request.repuestoId()
            );

        repuesto.consumirStock(request.cantidad());
        repuestoRepository.save(repuesto);

        LocalDateTime fecha = LocalDateTime.now();

        ConsumoRepuesto consumo =
            consumoRepuestoRepository.save(
                new ConsumoRepuesto(
                    orden,
                    repuesto,
                    actor,
                    request.cantidad(),
                    repuesto.getPrecioVenta(),
                    fecha
                )
            );

        movimientoStockRepository.save(
            new MovimientoStock(
                repuesto,
                actor,
                consumo,
                TipoMovimientoStock.CONSUMO,
                -request.cantidad(),
                "Consumo en orden de trabajo",
                "ORDEN-" + orden.getId(),
                fecha
            )
        );

        return convertirConsumoAResponse(consumo);
    }

    @Transactional(readOnly = true)
    public List<MovimientoStockResponse> listarMovimientos(
        Long actorId
    ) {
        Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        validarAdministrador(actor);

        return movimientoStockRepository
            .findAllByOrderByFechaDescIdDesc()
            .stream()
            .map(this::convertirMovimientoAResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimientoStockResponse> listarMovimientosPorRepuesto(
        Long repuestoId,
        Long actorId
    ) {
        Usuario actor = usuarioService.buscarEntidadPorId(actorId);
        validarAdministrador(actor);

        if (!repuestoRepository.existsById(repuestoId)) {
            throw new RepuestoNoEncontradoException(repuestoId);
        }

        return movimientoStockRepository
            .findAllByRepuestoIdOrderByFechaDescIdDesc(repuestoId)
            .stream()
            .map(this::convertirMovimientoAResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ConsumoRepuestoResponse> listarConsumosPorOrden(
        Long ordenId,
        Long actorId
    ) {
        Usuario actor = usuarioService.buscarEntidadPorId(actorId);

        OrdenTrabajo orden =
            ordenTrabajoService.buscarEntidadPorId(ordenId);

        validarConsultaConsumos(orden, actor);

        return consumoRepuestoRepository
            .findAllByOrdenIdOrderByFechaAscIdAsc(ordenId)
            .stream()
            .map(this::convertirConsumoAResponse)
            .toList();
    }

    private Repuesto buscarRepuestoActivoParaActualizar(
        Long repuestoId
    ) {
        return repuestoRepository
            .findActivoByIdForUpdate(repuestoId)
            .orElseThrow(
                () -> new RepuestoNoEncontradoException(repuestoId)
            );
    }

    private void validarAdministrador(Usuario actor) {
        if (!actor.tieneRol(NombreRol.ADMINISTRADOR)) {
            throw new AccessDeniedException(
                "La operación de stock requiere rol Administrador"
            );
        }
    }

    private void validarTecnicoAsignado(
        OrdenTrabajo orden,
        Usuario actor
    ) {
        Usuario tecnicoAsignado = orden.getTecnico();

        if (
            tecnicoAsignado == null
                || !tecnicoAsignado.getId().equals(actor.getId())
        ) {
            throw new AccessDeniedException(
                "El consumo debe registrarlo el Técnico asignado"
            );
        }
    }

    private void validarConsultaConsumos(
        OrdenTrabajo orden,
        Usuario actor
    ) {
        if (
            actor.tieneRol(NombreRol.ADMINISTRADOR)
                || actor.tieneRol(NombreRol.RECEPCIONISTA)
        ) {
            return;
        }

        Usuario tecnicoAsignado = orden.getTecnico();

        if (
            actor.tieneRol(NombreRol.TECNICO)
                && tecnicoAsignado != null
                && tecnicoAsignado.getId().equals(actor.getId())
        ) {
            return;
        }

        throw new AccessDeniedException(
            "No puede consultar los consumos de esta orden"
        );
    }

    private void validarEstadoParaConsumo(OrdenTrabajo orden) {
        NombreEstadoOrden estado =
            orden.getEstado().getNombre();

        if (estado != NombreEstadoOrden.EN_REPARACION) {
            throw new EstadoOrdenInvalidoParaConsumoException(
                estado
            );
        }
    }

    private String normalizarTextoOpcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }

    private MovimientoStockResponse convertirMovimientoAResponse(
        MovimientoStock movimiento
    ) {
        Repuesto repuesto = movimiento.getRepuesto();
        ConsumoRepuesto consumo = movimiento.getConsumo();

        return new MovimientoStockResponse(
            movimiento.getId(),
            repuesto.getId(),
            repuesto.getCodigo(),
            repuesto.getNombre(),
            movimiento.getUsuario().getId(),
            consumo == null ? null : consumo.getId(),
            movimiento.getTipo(),
            movimiento.getCantidad(),
            movimiento.getMotivo(),
            movimiento.getReferencia(),
            movimiento.getFecha()
        );
    }

    private ConsumoRepuestoResponse convertirConsumoAResponse(
        ConsumoRepuesto consumo
    ) {
        Repuesto repuesto = consumo.getRepuesto();

        return new ConsumoRepuestoResponse(
            consumo.getId(),
            consumo.getOrden().getId(),
            repuesto.getId(),
            repuesto.getCodigo(),
            repuesto.getNombre(),
            consumo.getUsuario().getId(),
            consumo.getCantidad(),
            consumo.getPrecioUnitario(),
            consumo.getFecha()
        );
    }
}
