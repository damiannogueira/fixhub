package fixhub.ordenes.config;

import fixhub.ordenes.entity.EstadoOrden;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.repository.EstadoOrdenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InicializadorEstadosOrdenTest {

    @Mock
    private EstadoOrdenRepository estadoOrdenRepository;

    @InjectMocks
    private InicializadorEstadosOrden inicializador;

    @Test
    void debeCrearLosNueveEstadosYMarcarLosFinales() {
        inicializador.run(null);

        ArgumentCaptor<EstadoOrden> captor =
            ArgumentCaptor.forClass(EstadoOrden.class);
        verify(estadoOrdenRepository, times(9)).save(captor.capture());

        List<EstadoOrden> estados = captor.getAllValues();
        assertThat(estados)
            .extracting(EstadoOrden::getNombre)
            .containsExactlyInAnyOrder(NombreEstadoOrden.values());
        assertThat(estados)
            .filteredOn(EstadoOrden::isEstadoFinal)
            .extracting(EstadoOrden::getNombre)
            .containsExactlyInAnyOrder(
                NombreEstadoOrden.ENTREGADA,
                NombreEstadoOrden.CANCELADA
            );
    }

    @Test
    void noDebeDuplicarEstadosExistentes() {
        when(estadoOrdenRepository.existsByNombre(any())).thenReturn(true);

        inicializador.run(null);

        verify(estadoOrdenRepository, never()).save(any());
    }
}
