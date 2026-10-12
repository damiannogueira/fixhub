package fixhub.inventario.repository;

import fixhub.inventario.entity.Repuesto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RepuestoRepositoryTest {

    @Autowired
    private RepuestoRepository repuestoRepository;

    @Test
    void debeEncontrarRepuestosConStockBajo() {
        Repuesto stockBajo = crearRepuesto(
            "REP-BAJO",
            "Batería",
            1,
            2
        );

        Repuesto stockAlLimite = crearRepuesto(
            "REP-LIMITE",
            "Pantalla",
            2,
            2
        );

        Repuesto stockSuficiente = crearRepuesto(
            "REP-SUFICIENTE",
            "Teclado",
            5,
            2
        );

        repuestoRepository.saveAllAndFlush(
            List.of(stockBajo, stockAlLimite, stockSuficiente)
        );

        List<Repuesto> resultado =
            repuestoRepository.findRepuestosConStockBajo();

        assertThat(resultado)
            .extracting(Repuesto::getCodigo)
            .containsExactly("REP-BAJO", "REP-LIMITE");
    }

    @Test
    void debeExcluirRepuestosDesactivadosDelListado() {
        Repuesto activo = crearRepuesto(
            "REP-ACTIVO",
            "Cable USB",
            5,
            2
        );

        Repuesto desactivado = crearRepuesto(
            "REP-INACTIVO",
            "Cargador antiguo",
            0,
            2
        );

        desactivado.setActivo(false);

        repuestoRepository.saveAllAndFlush(List.of(activo, desactivado));

        List<Repuesto> resultado =
            repuestoRepository.findAllByActivoTrueOrderByNombreAsc();

        assertThat(resultado)
            .extracting(Repuesto::getCodigo)
            .containsExactly("REP-ACTIVO");
    }

    @Test
    void debeBuscarCodigoSinDistinguirMayusculasYMinusculas() {
        Repuesto repuesto = crearRepuesto(
            "REP-001",
            "Pantalla",
            4,
            2
        );

        repuestoRepository.saveAndFlush(repuesto);

        boolean existe =
            repuestoRepository.existsByCodigoIgnoreCase("rep-001");

        assertThat(existe).isTrue();
    }

    private Repuesto crearRepuesto(
        String codigo,
        String nombre,
        int stockActual,
        int stockMinimo
    ) {
        Repuesto repuesto = new Repuesto(
            codigo,
            nombre,
            null,
            stockMinimo,
            new BigDecimal("80.00"),
            new BigDecimal("100.00")
        );

        repuesto.setStockActual(stockActual);

        return repuesto;
    }
}
