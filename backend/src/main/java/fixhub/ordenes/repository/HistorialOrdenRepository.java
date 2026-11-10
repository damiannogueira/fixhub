package fixhub.ordenes.repository;

import fixhub.ordenes.entity.HistorialOrden;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialOrdenRepository
    extends JpaRepository<HistorialOrden, Long> {

    List<HistorialOrden> findAllByOrdenIdOrderByFechaAscIdAsc(Long ordenId);
}
