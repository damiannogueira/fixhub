package fixhub.ordenes.config;

import fixhub.ordenes.entity.EstadoOrden;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.repository.EstadoOrdenRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
@ConditionalOnProperty(
    name = "app.bootstrap.estados-orden.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class InicializadorEstadosOrden implements ApplicationRunner {

    private final EstadoOrdenRepository estadoOrdenRepository;

    public InicializadorEstadosOrden(
        EstadoOrdenRepository estadoOrdenRepository
    ) {
        this.estadoOrdenRepository = estadoOrdenRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (NombreEstadoOrden nombre : NombreEstadoOrden.values()) {
            if (!estadoOrdenRepository.existsByNombre(nombre)) {
                estadoOrdenRepository.save(
                    new EstadoOrden(nombre, esFinal(nombre))
                );
            }
        }
    }

    private boolean esFinal(NombreEstadoOrden nombre) {
        return nombre == NombreEstadoOrden.ENTREGADA
            || nombre == NombreEstadoOrden.CANCELADA;
    }
}
