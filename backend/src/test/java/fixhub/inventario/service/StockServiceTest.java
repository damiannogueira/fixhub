package fixhub.inventario.service;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
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
import fixhub.inventario.exception.StockInsuficienteException;
import fixhub.inventario.repository.ConsumoRepuestoRepository;
import fixhub.inventario.repository.MovimientoStockRepository;
import fixhub.inventario.repository.RepuestoRepository;
import fixhub.ordenes.entity.EstadoOrden;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.entity.OrdenTrabajo;
import fixhub.ordenes.service.OrdenTrabajoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private RepuestoRepository repuestoRepository;

    @Mock
    private ConsumoRepuestoRepository consumoRepuestoRepository;

    @Mock
    private MovimientoStockRepository movimientoStockRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private OrdenTrabajoService ordenTrabajoService;

    @InjectMocks
    private StockService stockService;

    @Test
    void administradorDebeRegistrarEntradaYActualizarStock() {
        Usuario administrador =
            usuario(1L, NombreRol.ADMINISTRADOR);

        Repuesto repuesto = repuesto(10L, 5);

        when(usuarioService.buscarEntidadPorId(1L))
            .thenReturn(administrador);

        when(repuestoRepository.findActivoByIdForUpdate(10L))
            .thenReturn(Optional.of(repuesto));

        when(movimientoStockRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoStockResponse response =
            stockService.registrarEntrada(
                10L,
                new RegistrarEntradaStockRequest(
                    4,
                    " Compra de repuestos ",
                    " FACTURA-001 "
                ),
                1L
            );

        assertThat(repuesto.getStockActual()).isEqualTo(9);
        assertThat(response.tipo())
            .isEqualTo(TipoMovimientoStock.ENTRADA);
        assertThat(response.cantidad()).isEqualTo(4);
        assertThat(response.motivo())
            .isEqualTo("Compra de repuestos");
        assertThat(response.referencia())
            .isEqualTo("FACTURA-001");

        verify(repuestoRepository).save(repuesto);
    }

    @Test
    void usuarioNoAdministradorNoDebeRegistrarEntrada() {
        Usuario tecnico = usuario(2L, NombreRol.TECNICO);

        when(usuarioService.buscarEntidadPorId(2L))
            .thenReturn(tecnico);

        assertThatThrownBy(
            () -> stockService.registrarEntrada(
                10L,
                new RegistrarEntradaStockRequest(
                    4,
                    "Compra",
                    null
                ),
                2L
            )
        ).isInstanceOf(AccessDeniedException.class);

        verify(repuestoRepository, never())
            .findActivoByIdForUpdate(any());
        verify(movimientoStockRepository, never()).save(any());
    }

    @Test
    void administradorDebeAjustarStockYGuardarDiferencia() {
        Usuario administrador =
            usuario(1L, NombreRol.ADMINISTRADOR);

        Repuesto repuesto = repuesto(10L, 8);

        when(usuarioService.buscarEntidadPorId(1L))
            .thenReturn(administrador);

        when(repuestoRepository.findActivoByIdForUpdate(10L))
            .thenReturn(Optional.of(repuesto));

        when(movimientoStockRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoStockResponse response =
            stockService.ajustarStock(
                10L,
                new AjustarStockRequest(
                    5,
                    "Corrección de inventario",
                    null
                ),
                1L
            );

        assertThat(repuesto.getStockActual()).isEqualTo(5);
        assertThat(response.tipo())
            .isEqualTo(TipoMovimientoStock.AJUSTE);
        assertThat(response.cantidad()).isEqualTo(-3);
    }

    @Test
    void tecnicoAsignadoDebeRegistrarConsumo() {
        Usuario tecnico = usuario(2L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(
            20L,
            tecnico,
            NombreEstadoOrden.EN_REPARACION
        );
        Repuesto repuesto = repuesto(10L, 5);

        when(usuarioService.buscarEntidadPorId(2L))
            .thenReturn(tecnico);

        when(ordenTrabajoService.buscarEntidadPorId(20L))
            .thenReturn(orden);

        when(repuestoRepository.findActivoByIdForUpdate(10L))
            .thenReturn(Optional.of(repuesto));

        when(consumoRepuestoRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(movimientoStockRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ConsumoRepuestoResponse response =
            stockService.registrarConsumo(
                20L,
                new RegistrarConsumoRepuestoRequest(10L, 2),
                2L
            );

        assertThat(repuesto.getStockActual()).isEqualTo(3);
        assertThat(response.ordenId()).isEqualTo(20L);
        assertThat(response.repuestoId()).isEqualTo(10L);
        assertThat(response.usuarioId()).isEqualTo(2L);
        assertThat(response.cantidad()).isEqualTo(2);
        assertThat(response.precioUnitario())
            .isEqualByComparingTo("1500.00");

        ArgumentCaptor<MovimientoStock> captor =
            ArgumentCaptor.forClass(MovimientoStock.class);

        verify(movimientoStockRepository)
            .save(captor.capture());

        MovimientoStock movimiento = captor.getValue();

        assertThat(movimiento.getTipo())
            .isEqualTo(TipoMovimientoStock.CONSUMO);
        assertThat(movimiento.getCantidad()).isEqualTo(-2);
        assertThat(movimiento.getConsumo()).isNotNull();
    }

    @Test
    void noDebeRegistrarConsumoConStockInsuficiente() {
        Usuario tecnico = usuario(2L, NombreRol.TECNICO);
        OrdenTrabajo orden = orden(
            20L,
            tecnico,
            NombreEstadoOrden.EN_REPARACION
        );
        Repuesto repuesto = repuesto(10L, 1);

        when(usuarioService.buscarEntidadPorId(2L))
            .thenReturn(tecnico);

        when(ordenTrabajoService.buscarEntidadPorId(20L))
            .thenReturn(orden);

        when(repuestoRepository.findActivoByIdForUpdate(10L))
            .thenReturn(Optional.of(repuesto));

        assertThatThrownBy(
            () -> stockService.registrarConsumo(
                20L,
                new RegistrarConsumoRepuestoRequest(10L, 2),
                2L
            )
        ).isInstanceOf(StockInsuficienteException.class);

        assertThat(repuesto.getStockActual()).isEqualTo(1);

        verify(repuestoRepository, never()).save(any());
        verify(consumoRepuestoRepository, never()).save(any());
        verify(movimientoStockRepository, never()).save(any());
    }

    @Test
    void tecnicoAjenoDebeSerRechazadoAntesDeValidarEstado() {
        Usuario asignado = usuario(2L, NombreRol.TECNICO);
        Usuario ajeno = usuario(3L, NombreRol.TECNICO);

        OrdenTrabajo orden = orden(
            20L,
            asignado,
            NombreEstadoOrden.RECIBIDA
        );

        when(usuarioService.buscarEntidadPorId(3L))
            .thenReturn(ajeno);

        when(ordenTrabajoService.buscarEntidadPorId(20L))
            .thenReturn(orden);

        assertThatThrownBy(
            () -> stockService.registrarConsumo(
                20L,
                new RegistrarConsumoRepuestoRequest(10L, 1),
                3L
            )
        ).isInstanceOf(AccessDeniedException.class);

        verify(repuestoRepository, never())
            .findActivoByIdForUpdate(any());
        verify(consumoRepuestoRepository, never()).save(any());
    }

    private Usuario usuario(Long id, NombreRol rol) {
        Usuario usuario = new Usuario(
            "hash",
            "usuario" + id + "@fixhub.com",
            null
        );

        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.agregarRol(new Rol(rol));

        return usuario;
    }

    private Repuesto repuesto(Long id, Integer stockActual) {
        Repuesto repuesto = new Repuesto(
            "REP-" + id,
            "Fuente de alimentación",
            null,
            2,
            new BigDecimal("1000.00"),
            new BigDecimal("1500.00")
        );

        ReflectionTestUtils.setField(repuesto, "id", id);
        repuesto.ajustarStock(stockActual);

        return repuesto;
    }

    private OrdenTrabajo orden(
        Long id,
        Usuario tecnico,
        NombreEstadoOrden nombreEstado
    ) {
        EstadoOrden estado = new EstadoOrden(
            nombreEstado,
            false
        );

        OrdenTrabajo orden = new OrdenTrabajo(
            null,
            tecnico,
            estado,
            "No enciende",
            LocalDateTime.now()
        );

        ReflectionTestUtils.setField(orden, "id", id);

        return orden;
    }
}
