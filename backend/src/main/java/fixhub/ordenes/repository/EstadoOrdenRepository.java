package fixhub.ordenes.repository;

import fixhub.ordenes.entity.EstadoOrden;
import fixhub.ordenes.entity.NombreEstadoOrden;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoOrdenRepository
    extends JpaRepository<EstadoOrden, Long> {

    boolean existsByNombre(NombreEstadoOrden nombre);

    Optional<EstadoOrden> findByNombre(NombreEstadoOrden nombre);
}
