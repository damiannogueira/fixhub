package fixhub.inventario.service;

import fixhub.inventario.dto.CrearRepuestoRequest;
import fixhub.inventario.dto.RepuestoResponse;
import fixhub.inventario.entity.Repuesto;
import fixhub.inventario.exception.CodigoRepuestoDuplicadoException;
import fixhub.inventario.exception.RepuestoNoEncontradoException;
import fixhub.inventario.repository.RepuestoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepuestoServiceTest {

    @Mock
    private RepuestoRepository repuestoRepository;

    @InjectMocks
    private RepuestoService repuestoService;

    @Test
    void debeCrearRepuestoNormalizandoLosDatos() {
        CrearRepuestoRequest request = new CrearRepuestoRequest(
            " rep-001 ",
            " Pantalla 15 pulgadas ",
            " Pantalla compatible ",
            2,
            new BigDecimal("80000.00"),
            new BigDecimal("125000.00")
        );

        when(repuestoRepository.existsByCodigoIgnoreCase("REP-001"))
            .thenReturn(false);

        when(repuestoRepository.save(any(Repuesto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        RepuestoResponse response = repuestoService.crear(request);

        assertThat(response.codigo()).isEqualTo("REP-001");
        assertThat(response.nombre()).isEqualTo("Pantalla 15 pulgadas");
        assertThat(response.descripcion()).isEqualTo("Pantalla compatible");
        assertThat(response.stockActual()).isZero();
        assertThat(response.stockMinimo()).isEqualTo(2);

        assertThat(response.precioCosto())
            .isEqualByComparingTo("80000.00");

        assertThat(response.precioVenta())
            .isEqualByComparingTo("125000.00");

        assertThat(response.activo()).isTrue();

        ArgumentCaptor<Repuesto> captor =
            ArgumentCaptor.forClass(Repuesto.class);

        verify(repuestoRepository).save(captor.capture());

        assertThat(captor.getValue().getCodigo()).isEqualTo("REP-001");
    }

    @Test
    void noDebeCrearUnRepuestoConCodigoDuplicado() {
        CrearRepuestoRequest request = new CrearRepuestoRequest(
            "REP-001",
            "Pantalla",
            null,
            2,
            new BigDecimal("80000.00"),
            new BigDecimal("125000.00")
        );

        when(repuestoRepository.existsByCodigoIgnoreCase("REP-001"))
            .thenReturn(true);

        assertThatThrownBy(() -> repuestoService.crear(request))
            .isInstanceOf(CodigoRepuestoDuplicadoException.class)
            .hasMessageContaining("REP-001");

        verify(repuestoRepository, never()).save(any(Repuesto.class));
    }

    @Test
    void debeLanzarExcepcionSiElRepuestoNoExiste() {
        when(repuestoRepository.findByIdAndActivoTrue(99L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> repuestoService.buscarPorId(99L))
            .isInstanceOf(RepuestoNoEncontradoException.class)
            .hasMessageContaining("99");
    }

    @Test
    void debeRealizarLaBajaLogicaDelRepuesto() {
        Repuesto repuesto = new Repuesto(
            "REP-001",
            "Pantalla",
            null,
            2,
            new BigDecimal("80000.00"),
            new BigDecimal("125000.00")
        );

        when(repuestoRepository.findByIdAndActivoTrue(1L))
            .thenReturn(Optional.of(repuesto));

        repuestoService.desactivar(1L);

        assertThat(repuesto.getActivo()).isFalse();
        verify(repuestoRepository).save(repuesto);
    }
}
