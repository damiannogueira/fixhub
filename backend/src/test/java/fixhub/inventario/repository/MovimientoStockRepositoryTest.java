package fixhub.inventario.repository;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.repository.RolRepository;
import fixhub.autenticacion.repository.UsuarioRepository;
import fixhub.clientes.entity.Cliente;
import fixhub.clientes.entity.Equipo;
import fixhub.clientes.repository.ClienteRepository;
import fixhub.clientes.repository.EquipoRepository;
import fixhub.inventario.entity.ConsumoRepuesto;
import fixhub.inventario.entity.MovimientoStock;
import fixhub.inventario.entity.Repuesto;
import fixhub.inventario.entity.TipoMovimientoStock;
import fixhub.ordenes.entity.EstadoOrden;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.entity.OrdenTrabajo;
import fixhub.ordenes.repository.EstadoOrdenRepository;
import fixhub.ordenes.repository.OrdenTrabajoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MovimientoStockRepositoryTest {

    @Autowired
    private ConsumoRepuestoRepository consumoRepuestoRepository;

    @Autowired
    private MovimientoStockRepository movimientoStockRepository;

    @Autowired
    private RepuestoRepository repuestoRepository;

    @Autowired
    private OrdenTrabajoRepository ordenTrabajoRepository;

    @Autowired
    private EstadoOrdenRepository estadoOrdenRepository;

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    private EstadoOrden enReparacion;
    private Rol rolTecnico;

    @BeforeEach
    void configurar() {
        enReparacion = estadoOrdenRepository.saveAndFlush(
            new EstadoOrden(
                NombreEstadoOrden.EN_REPARACION,
                false
            )
        );

        rolTecnico = rolRepository.saveAndFlush(
            new Rol(NombreRol.TECNICO)
        );
    }

    @Test
    void debePersistirConsumoYMovimientoRelacionado() {
        Usuario tecnico = guardarUsuario("tecnico.stock@fixhub.com");
        tecnico.agregarRol(rolTecnico);
        usuarioRepository.saveAndFlush(tecnico);

        OrdenTrabajo orden = guardarOrden(tecnico);
        Repuesto repuesto = guardarRepuesto("REP-001");
        LocalDateTime fecha = LocalDateTime.now();

        ConsumoRepuesto consumo =
            consumoRepuestoRepository.saveAndFlush(
                new ConsumoRepuesto(
                    orden,
                    repuesto,
                    tecnico,
                    2,
                    new BigDecimal("1500.00"),
                    fecha
                )
            );

        MovimientoStock movimiento =
            movimientoStockRepository.saveAndFlush(
                new MovimientoStock(
                    repuesto,
                    tecnico,
                    consumo,
                    TipoMovimientoStock.CONSUMO,
                    -2,
                    "Consumo en reparación",
                    "ORDEN-" + orden.getId(),
                    fecha
                )
            );

        assertThat(consumo.getOrden()).isEqualTo(orden);
        assertThat(consumo.getRepuesto()).isEqualTo(repuesto);
        assertThat(consumo.getUsuario()).isEqualTo(tecnico);
        assertThat(consumo.getCantidad()).isEqualTo(2);

        assertThat(movimiento.getConsumo()).isEqualTo(consumo);
        assertThat(movimiento.getTipo())
            .isEqualTo(TipoMovimientoStock.CONSUMO);
        assertThat(movimiento.getCantidad()).isEqualTo(-2);

        assertThat(
            consumoRepuestoRepository
                .findAllByOrdenIdOrderByFechaAscIdAsc(
                    orden.getId()
                )
        ).containsExactly(consumo);

        assertThat(
            movimientoStockRepository
                .findAllByRepuestoIdOrderByFechaDescIdDesc(
                    repuesto.getId()
                )
        ).containsExactly(movimiento);
    }

    @Test
    void debePersistirEntradaSinConsumoRelacionado() {
        Usuario administrador =
            guardarUsuario("admin.stock@fixhub.com");

        Repuesto repuesto = guardarRepuesto("REP-002");

        MovimientoStock movimiento =
            movimientoStockRepository.saveAndFlush(
                new MovimientoStock(
                    repuesto,
                    administrador,
                    null,
                    TipoMovimientoStock.ENTRADA,
                    10,
                    "Compra inicial",
                    "FACTURA-001",
                    LocalDateTime.now()
                )
            );

        assertThat(movimiento.getConsumo()).isNull();
        assertThat(movimiento.getTipo())
            .isEqualTo(TipoMovimientoStock.ENTRADA);
        assertThat(movimiento.getCantidad()).isEqualTo(10);

        assertThat(
            movimientoStockRepository
                .findAllByOrderByFechaDescIdDesc()
        ).containsExactly(movimiento);
    }

    private OrdenTrabajo guardarOrden(Usuario tecnico) {
        Usuario clienteUsuario =
            guardarUsuario("cliente.stock@fixhub.com");

        Cliente cliente = clienteRepository.saveAndFlush(
            new Cliente(
                clienteUsuario,
                "Ana",
                "Pérez",
                null,
                null
            )
        );

        Equipo equipo = equipoRepository.saveAndFlush(
            new Equipo(
                cliente,
                "Notebook",
                "Dell",
                "XPS",
                null,
                null
            )
        );

        return ordenTrabajoRepository.saveAndFlush(
            new OrdenTrabajo(
                equipo,
                tecnico,
                enReparacion,
                "No enciende",
                LocalDateTime.now()
            )
        );
    }

    private Repuesto guardarRepuesto(String codigo) {
        return repuestoRepository.saveAndFlush(
            new Repuesto(
                codigo,
                "Fuente de alimentación",
                null,
                2,
                new BigDecimal("1000.00"),
                new BigDecimal("1500.00")
            )
        );
    }

    private Usuario guardarUsuario(String email) {
        return usuarioRepository.saveAndFlush(
            new Usuario("hash", email, null)
        );
    }
}
